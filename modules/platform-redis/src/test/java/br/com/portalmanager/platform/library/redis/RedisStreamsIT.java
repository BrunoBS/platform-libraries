package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.redis.annotation.RedisStreamListener;
import br.com.portalmanager.platform.library.redis.core.RedisListenerMetrics;
import br.com.portalmanager.platform.library.redis.internal.RedisStreamsLifecycle;
import br.com.portalmanager.platform.library.redis.stream.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class RedisStreamsIT extends RedisIntegrationSupport {
    @RedisStreamListener(
            stream = "listener.orders",
            group = "workers",
            concurrency = 2,
            batchSize = 2,
            minIdleMillis = 50,
            recoveryIntervalMillis = 50)
    static class Listener implements RedisStreamMessageListener<Event> {
        final AtomicInteger attempts = new AtomicInteger();
        final BlockingQueue<Event> done = new LinkedBlockingQueue<>();

        public void onMessage(Event event) {
            if (attempts.getAndIncrement() == 0) throw new RuntimeException("private");
            done.add(event);
        }
    }

    @Test
    void passiveBootstrapDoesNotPoll() {
        context()
                .withPropertyValues("spring.data.redis.port=1")
                .run(
                        ctx ->
                                assertThat(ctx.getBean(RedisStreamsLifecycle.class).isRunning())
                                        .isFalse());
    }

    @Test
    void appendReadGroupsAckPendingRecoveryAndRetention() {
        context()
                .run(
                        ctx -> {
                            var streams = ctx.getBean(RedisStreams.class);
                            String key = key();
                            streams.createGroup(key, "group", GroupStart.BEGINNING);
                            streams.createGroup(key, "group", GroupStart.LATEST);
                            String first = streams.append(key, new Event("first", 1), 3);
                            streams.append(key, new Event("second", 2), 3);
                            assertThat(streams.read(key, "0-0", 2, Duration.ZERO, Event.class))
                                    .extracting(RedisStreamRecord::id)
                                    .contains(first);
                            var read =
                                    streams.readGroup(
                                            key,
                                            "group",
                                            "first-consumer",
                                            2,
                                            Duration.ZERO,
                                            Event.class);
                            assertThat(read).hasSize(2);
                            assertThat(
                                            streams.readGroup(
                                                    key,
                                                    "group",
                                                    "first-consumer",
                                                    2,
                                                    Duration.ZERO,
                                                    Event.class))
                                    .isEmpty();
                            await().atMost(Duration.ofSeconds(2))
                                    .untilAsserted(
                                            () ->
                                                    assertThat(
                                                                    streams.recover(
                                                                            key,
                                                                            "group",
                                                                            "second-consumer",
                                                                            Duration.ofMillis(1),
                                                                            2,
                                                                            Event.class))
                                                            .hasSize(2));
                            assertThat(
                                            streams.acknowledge(
                                                    key,
                                                    "group",
                                                    read.stream()
                                                            .map(RedisStreamRecord::id)
                                                            .toList()))
                                    .isEqualTo(2);
                            assertThat(
                                            streams.recover(
                                                    key,
                                                    "group",
                                                    "third",
                                                    Duration.ofMillis(1),
                                                    2,
                                                    Event.class))
                                    .isEmpty();
                            streams.append(key, new Event("third", 3), 3);
                            streams.append(key, new Event("fourth", 4), 3);
                            assertThat(streams.read(key, "0-0", 10, Duration.ZERO, Event.class))
                                    .hasSize(3);
                            assertThat(streams.trim(key, 1)).isEqualTo(2);
                        });
    }

    @Test
    void latestGroupSkipsExistingAndReadLimitsAreValidated() {
        context()
                .run(
                        ctx -> {
                            var streams = ctx.getBean(RedisStreams.class);
                            String key = key();
                            streams.append(key, "old");
                            streams.createGroup(key, "latest", GroupStart.LATEST);
                            assertThat(
                                            streams.readGroup(
                                                    key,
                                                    "latest",
                                                    "consumer",
                                                    1,
                                                    Duration.ZERO,
                                                    String.class))
                                    .isEmpty();
                            streams.append(key, "new");
                            assertThat(
                                            streams.readGroup(
                                                    key,
                                                    "latest",
                                                    "consumer",
                                                    1,
                                                    Duration.ZERO,
                                                    String.class))
                                    .extracting(RedisStreamRecord::payload)
                                    .containsExactly("new");
                            assertThatThrownBy(
                                            () ->
                                                    streams.read(
                                                            key,
                                                            "0-0",
                                                            1001,
                                                            Duration.ZERO,
                                                            String.class))
                                    .isInstanceOf(
                                            br.com.portalmanager.platform.library.messaging
                                                    .exception.ApiException.class);
                            assertThatThrownBy(
                                            () ->
                                                    streams.read(
                                                            key,
                                                            "0-0",
                                                            1,
                                                            Duration.ofSeconds(6),
                                                            String.class))
                                    .isInstanceOf(
                                            br.com.portalmanager.platform.library.messaging
                                                    .exception.ApiException.class);
                        });
    }

    @Test
    void listenerAcknowledgesOnlyAfterSuccessAndRecoversFailure() {
        var listener = new Listener();
        context()
                .withBean(Listener.class, () -> listener)
                .run(
                        ctx -> {
                            var streams = ctx.getBean(RedisStreams.class);
                            streams.append("listener.orders", new Event("recover", 1));
                            assertThat(listener.done.poll(4, TimeUnit.SECONDS))
                                    .isEqualTo(new Event("recover", 1));
                            assertThat(listener.attempts.get()).isGreaterThanOrEqualTo(2);
                            assertThat(ctx.getBean(RedisListenerMetrics.class).failureCount())
                                    .isPositive();
                            await().atMost(Duration.ofSeconds(3))
                                    .untilAsserted(
                                            () ->
                                                    assertThat(
                                                                    ctx.getBean(
                                                                                    StringRedisTemplate
                                                                                            .class)
                                                                            .opsForStream()
                                                                            .pending(
                                                                                    "redis-test:listener.orders",
                                                                                    "workers")
                                                                            .getTotalPendingMessages())
                                                            .isZero());
                            ctx.getBean(RedisStreamsLifecycle.class).stop();
                            assertThat(ctx.getBean(RedisStreamsLifecycle.class).isRunning())
                                    .isFalse();
                        });
    }

    @RedisStreamListener(
            stream = "listener.malformed",
            group = "malformed",
            batchSize = 2,
            minIdleMillis = 10000,
            recoveryIntervalMillis = 100)
    static class Malformed implements RedisStreamMessageListener<Event> {
        final BlockingQueue<Event> done = new LinkedBlockingQueue<>();

        public void onMessage(Event event) {
            done.add(event);
        }
    }

    @Test
    void malformedRecordRemainsPendingAndDoesNotBlockValidRecordInBatch() {
        var listener = new Malformed();
        context()
                .withBean(Malformed.class, () -> listener)
                .run(
                        ctx -> {
                            ctx.getBean(StringRedisTemplate.class)
                                    .opsForStream()
                                    .add(
                                            "redis-test:listener.malformed",
                                            Map.of("payload", "INVALID_JSON"));
                            ctx.getBean(RedisStreams.class)
                                    .append("listener.malformed", new Event("valid", 1));
                            assertThat(listener.done.poll(3, TimeUnit.SECONDS))
                                    .isEqualTo(new Event("valid", 1));
                            await().atMost(Duration.ofSeconds(2))
                                    .untilAsserted(
                                            () ->
                                                    assertThat(
                                                                    ctx.getBean(
                                                                                    StringRedisTemplate
                                                                                            .class)
                                                                            .opsForStream()
                                                                            .pending(
                                                                                    "redis-test:listener.malformed",
                                                                                    "malformed")
                                                                            .getTotalPendingMessages())
                                                            .isEqualTo(1));
                        });
    }

    @Test
    void boundedRecoveryCursorAdvancesPastRecentPendingToOlderEligibleRecords() {
        context()
                .run(
                        ctx -> {
                            var streams = ctx.getBean(RedisStreams.class);
                            String key = key();
                            streams.createGroup(key, "cursor", GroupStart.BEGINNING);
                            for (int index = 0; index < 5; index++)
                                streams.append(key, new Event("item", index));
                            var read =
                                    streams.readGroup(
                                            key,
                                            "cursor",
                                            "original",
                                            5,
                                            Duration.ZERO,
                                            Event.class);
                            var nativeOps = ctx.getBean(StringRedisTemplate.class).opsForStream();
                            await().atMost(Duration.ofSeconds(3))
                                    .untilAsserted(
                                            () ->
                                                    assertThat(
                                                                    nativeOps
                                                                            .pending(
                                                                                    "redis-test:"
                                                                                            + key,
                                                                                    "cursor",
                                                                                    Range
                                                                                            .unbounded(),
                                                                                    5)
                                                                            .get(4)
                                                                            .getElapsedTimeSinceLastDelivery())
                                                            .isGreaterThanOrEqualTo(
                                                                    Duration.ofMillis(1200)));
                            nativeOps.claim(
                                    "redis-test:" + key,
                                    "cursor",
                                    "active",
                                    Duration.ZERO,
                                    org.springframework.data.redis.connection.stream.RecordId.of(
                                            read.get(0).id()),
                                    org.springframework.data.redis.connection.stream.RecordId.of(
                                            read.get(1).id()));
                            var first =
                                    streams.recover(
                                            key,
                                            "cursor",
                                            "recovering",
                                            "0-0",
                                            Duration.ofSeconds(1),
                                            2,
                                            Event.class);
                            assertThat(first.nextId()).isEqualTo(read.get(1).id());
                            assertThat(first.records()).isEmpty();
                            var second =
                                    streams.recover(
                                            key,
                                            "cursor",
                                            "recovering",
                                            first.nextId(),
                                            Duration.ofSeconds(1),
                                            2,
                                            Event.class);
                            assertThat(second.records())
                                    .extracting(RedisStreamRecord::id)
                                    .containsExactly(read.get(2).id(), read.get(3).id());
                            var last =
                                    streams.recover(
                                            key,
                                            "cursor",
                                            "recovering",
                                            second.nextId(),
                                            Duration.ofSeconds(1),
                                            2,
                                            Event.class);
                            assertThat(last.records())
                                    .extracting(RedisStreamRecord::id)
                                    .containsExactly(read.get(4).id());
                            assertThat(last.nextId()).isEqualTo("0-0");
                        });
    }
}
