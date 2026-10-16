package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.redis.annotation.RedisSubscribe;
import br.com.portalmanager.platform.library.redis.core.*;
import br.com.portalmanager.platform.library.redis.internal.RedisPubSubLifecycle;
import br.com.portalmanager.platform.library.redis.pubsub.*;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.*;

class RedisPubSubIT extends RedisIntegrationSupport {
    @RedisSubscribe(channels = {"events.one", "events.two"})
    static class Direct implements RedisMessageListener<Event> {
        final BlockingQueue<Event> events = new LinkedBlockingQueue<>();

        public void onMessage(Event event) {
            events.add(event);
        }
    }

    @RedisSubscribe(patterns = {"events.*"})
    static class Pattern implements RedisMessageListener<Event> {
        final BlockingQueue<Event> events = new LinkedBlockingQueue<>();

        public void onMessage(Event event) {
            events.add(event);
        }
    }

    @RedisSubscribe(channels = {"events.fail"})
    static class Broken implements RedisMessageListener<Event> {
        public void onMessage(Event event) {
            throw new RuntimeException("SENSITIVE_PAYLOAD");
        }
    }

    @RedisSubscribe(channels = {"events.slow"})
    static class Slow implements RedisMessageListener<Event> {
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);

        public void onMessage(Event event) {
            entered.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void passiveBootstrapHasNoSubscriberAndStoreDoesNotPublish() {
        context()
                .withPropertyValues("spring.data.redis.port=1")
                .run(
                        ctx ->
                                assertThat(ctx.getBean(RedisPubSubLifecycle.class).isRunning())
                                        .isFalse());
        context()
                .run(
                        ctx ->
                                assertThat(
                                                ctx.getBean(RedisPublisher.class)
                                                        .publish(
                                                                "events.none",
                                                                new Event("none", 0)))
                                        .isZero());
    }

    @Test
    void discoversMultipleListenersChannelsPatternsAndInstances() {
        var first = new Direct();
        var pattern = new Pattern();
        var second = new Direct();
        context()
                .withBean("direct", Direct.class, () -> first)
                .withBean(Pattern.class, () -> pattern)
                .run(
                        a -> {
                            context()
                                    .withBean("second", Direct.class, () -> second)
                                    .run(
                                            b -> {
                                                var publisher = a.getBean(RedisPublisher.class);
                                                assertThat(
                                                                publisher.publish(
                                                                        "events.one",
                                                                        new Event("one", 1)))
                                                        .isGreaterThanOrEqualTo(2);
                                                assertThat(first.events.poll(3, TimeUnit.SECONDS))
                                                        .isEqualTo(new Event("one", 1));
                                                assertThat(pattern.events.poll(3, TimeUnit.SECONDS))
                                                        .isEqualTo(new Event("one", 1));
                                                assertThat(second.events.poll(3, TimeUnit.SECONDS))
                                                        .isEqualTo(new Event("one", 1));
                                                a.getBean(RedisStore.class)
                                                        .set("unrelated", new Event("store", 1));
                                                assertThat(first.events.poll()).isNull();
                                                publisher.publish(
                                                        "events.two", new Event("two", 2));
                                                assertThat(first.events.poll(3, TimeUnit.SECONDS))
                                                        .isEqualTo(new Event("two", 2));
                                            });
                        });
    }

    @Test
    void stopDisconnectAndRestartDoesNotReplayTransientMessages() {
        var listener = new Direct();
        context()
                .withBean(Direct.class, () -> listener)
                .run(
                        ctx -> {
                            var lifecycle = ctx.getBean(RedisPubSubLifecycle.class);
                            var publisher = ctx.getBean(RedisPublisher.class);
                            lifecycle.stop();
                            assertThat(lifecycle.isRunning()).isFalse();
                            assertThat(publisher.publish("events.one", new Event("lost", 1)))
                                    .isZero();
                            lifecycle.start();
                            publisher.publish("events.one", new Event("after", 2));
                            assertThat(listener.events.poll(3, TimeUnit.SECONDS))
                                    .isEqualTo(new Event("after", 2));
                            assertThat(listener.events).isEmpty();
                        });
    }

    @Test
    void handlerFailureIsObservableAndSanitized() {
        var observed =
                new LinkedBlockingQueue<
                        br.com.portalmanager.platform.library.messaging.exception.ApiException>();
        context()
                .withBean(Broken.class, Broken::new)
                .withBean(
                        RedisListenerErrorHandler.class,
                        () -> (transport, error) -> observed.add(error))
                .run(
                        ctx -> {
                            ctx.getBean(RedisPublisher.class)
                                    .publish("events.fail", new Event("secret", 1));
                            var error = observed.poll(3, TimeUnit.SECONDS);
                            assertThat(error).isNotNull();
                            assertThat(error.getMessageKey()).isEqualTo("redis.listener");
                            assertThat(error.getCause().getMessage())
                                    .doesNotContain("SENSITIVE_PAYLOAD", "secret");
                            assertThat(ctx.getBean(RedisListenerMetrics.class).failureCount())
                                    .isPositive();
                        });
    }

    @Test
    void boundedQueueDropsWithExplicitObservableSaturation() {
        var slow = new Slow();
        context()
                .withPropertyValues(
                        "platform.redis.listener-threads=1",
                        "platform.redis.listener-queue-capacity=1")
                .withBean(Slow.class, () -> slow)
                .run(
                        ctx -> {
                            var publisher = ctx.getBean(RedisPublisher.class);
                            publisher.publish("events.slow", new Event("one", 1));
                            assertThat(slow.entered.await(3, TimeUnit.SECONDS)).isTrue();
                            for (int i = 0; i < 20; i++)
                                publisher.publish("events.slow", new Event("more", i));
                            await().atMost(Duration.ofSeconds(3))
                                    .untilAsserted(
                                            () ->
                                                    assertThat(
                                                                    ctx.getBean(
                                                                                    RedisListenerMetrics
                                                                                            .class)
                                                                            .rejectedCount())
                                                            .isPositive());
                            slow.release.countDown();
                        });
    }

    @Test
    void automaticallyReconnectsAfterRedisClosesSubscriptionConnection() {
        var listener = new Direct();
        context()
                .withBean(Direct.class, () -> listener)
                .run(
                        ctx -> {
                            var redis =
                                    ctx.getBean(
                                            org.springframework.data.redis.core.StringRedisTemplate
                                                    .class);
                            assertThat(
                                            REDIS.execInContainer(
                                                            "redis-cli",
                                                            "CLIENT",
                                                            "KILL",
                                                            "TYPE",
                                                            "PUBSUB")
                                                    .getExitCode())
                                    .isZero();
                            await().atMost(Duration.ofSeconds(6))
                                    .untilAsserted(
                                            () -> {
                                                ctx.getBean(RedisPublisher.class)
                                                        .publish(
                                                                "events.one",
                                                                new Event("reconnected", 1));
                                                assertThat(
                                                                listener.events.poll(
                                                                        100, TimeUnit.MILLISECONDS))
                                                        .isEqualTo(new Event("reconnected", 1));
                                            });
                        });
    }

    @RedisSubscribe(channels = {"events.generic"})
    static class Generic extends GenericBase<java.util.List<Event>> {}

    static class GenericBase<T> implements RedisMessageListener<T> {
        final BlockingQueue<T> values = new LinkedBlockingQueue<>();

        public void onMessage(T value) {
            values.add(value);
        }
    }

    @Test
    void resolvesInheritedParameterizedListenerType() {
        var listener = new Generic();
        context()
                .withBean(Generic.class, () -> listener)
                .run(
                        ctx -> {
                            ctx.getBean(RedisPublisher.class)
                                    .publish(
                                            "events.generic",
                                            java.util.List.of(new Event("typed", 1)));
                            assertThat(listener.values.poll(3, TimeUnit.SECONDS))
                                    .containsExactly(new Event("typed", 1));
                        });
    }
}
