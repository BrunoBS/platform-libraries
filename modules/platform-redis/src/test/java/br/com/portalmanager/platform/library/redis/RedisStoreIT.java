package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.*;
import br.com.portalmanager.platform.library.redis.core.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

class RedisStoreIT extends RedisIntegrationSupport {
    @Test
    void bootsAndRoundTripsDtosParameterizedListsAndMaps() {
        context()
                .run(
                        ctx -> {
                            assertThat(ctx)
                                    .hasSingleBean(RedisStore.class)
                                    .hasSingleBean(AsyncRedisStore.class);
                            var store = ctx.getBean(RedisStore.class);
                            String key = key();
                            assertThat(store.get(key, Event.class)).isEmpty();
                            store.set(key, new Event("alpha", 2));
                            assertThat(store.get(key, Event.class)).contains(new Event("alpha", 2));
                            store.set(key, List.of(new Event("beta", 3)));
                            assertThat(store.get(key, new TypeReference<List<Event>>() {}))
                                    .contains(List.of(new Event("beta", 3)));
                            store.set(key, Map.of("a", List.of(new Event("gamma", 4))));
                            assertThat(
                                            store.get(
                                                    key,
                                                    new TypeReference<
                                                            Map<String, List<Event>>>() {}))
                                    .contains(Map.of("a", List.of(new Event("gamma", 4))));
                            assertThat(store.delete(key)).isTrue();
                            assertThat(store.delete(key)).isFalse();
                        });
    }

    @Test
    void ttlOverwriteNxAndBuilderSingleUse() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            String key = key();
                            var write =
                                    store.write(key, "first").ttl(Duration.ofSeconds(3)).ifAbsent();
                            assertThat(write.execute()).isTrue();
                            assertThat(store.ttl(key)).isPresent();
                            assertThatThrownBy(write::execute)
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.builder.used");
                            assertThatThrownBy(write::ifAbsent).isInstanceOf(ApiException.class);
                            assertThat(store.setIfAbsent(key, "second")).isFalse();
                            store.set(key, "persistent");
                            assertThat(store.ttl(key)).isEmpty();
                            assertThat(store.expire(key, Duration.ofSeconds(3))).isTrue();
                            assertThat(store.persist(key)).isTrue();
                            assertThat(store.exists(key)).isTrue();
                            store.delete(key);
                            assertThat(store.ttl(key)).isEmpty();
                            assertThat(store.expire(key, Duration.ofSeconds(1))).isFalse();
                        });
    }

    @Test
    void concurrentNxHasOneWinner() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            String key = key();
                            try (var executor = Executors.newFixedThreadPool(8)) {
                                var futures = new ArrayList<Future<Boolean>>();
                                for (int i = 0; i < 24; i++)
                                    futures.add(
                                            executor.submit(() -> store.setIfAbsent(key, "one")));
                                int winners = 0;
                                for (var future : futures) if (future.get()) winners++;
                                assertThat(winners).isEqualTo(1);
                            }
                        });
    }

    @Test
    void invalidNullJsonAndNativeTypeAreNotMissing() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var nativeRedis = ctx.getBean(StringRedisTemplate.class);
                            String key = key();
                            assertThatThrownBy(() -> store.set(key, null))
                                    .isInstanceOf(ValidationException.class);
                            assertThatThrownBy(() -> store.set(key, "x", Duration.ZERO))
                                    .isInstanceOf(ValidationException.class);
                            nativeRedis.opsForValue().set("redis-test:" + key, "{broken");
                            assertThatThrownBy(() -> store.get(key, Event.class))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.serialization");
                            nativeRedis.opsForValue().set("redis-test:" + key, "null");
                            assertThatThrownBy(() -> store.get(key, Event.class))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.serialization");
                            store.set(key, 42);
                            assertThatThrownBy(() -> store.get(key, String.class))
                                    .isInstanceOf(ApiException.class);
                            store.delete(key);
                            nativeRedis.opsForList().leftPush("redis-test:" + key, "x");
                            assertThatThrownBy(() -> store.get(key, String.class))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.type");
                        });
    }

    @Test
    void asyncUsesRealReactiveIoAndPropagatesFailures() {
        context()
                .run(
                        ctx -> {
                            var async = ctx.getBean(AsyncRedisStore.class);
                            String key = key();
                            async.set(key, List.of(new Event("async", 1)))
                                    .toCompletableFuture()
                                    .get(3, TimeUnit.SECONDS);
                            assertThat(
                                            async.get(key, new TypeReference<List<Event>>() {})
                                                    .toCompletableFuture()
                                                    .get())
                                    .contains(List.of(new Event("async", 1)));
                            assertThat(async.setIfAbsent(key, "nx").toCompletableFuture().get())
                                    .isFalse();
                            assertThat(async.delete(key).toCompletableFuture().get()).isTrue();
                            assertThat(async.get(key, Event.class).toCompletableFuture().get())
                                    .isEmpty();
                            assertThatThrownBy(
                                            () -> async.set(key, null).toCompletableFuture().join())
                                    .hasCauseInstanceOf(ValidationException.class);
                        });
    }

    @Test
    void unavailableFailsAsyncWithoutBlockingCaller() {
        context()
                .withPropertyValues(
                        "spring.data.redis.port=1",
                        "spring.data.redis.timeout=100ms",
                        "spring.data.redis.connect-timeout=100ms")
                .run(
                        ctx -> {
                            var stage =
                                    ctx.getBean(AsyncRedisStore.class)
                                            .get("missing", String.class)
                                            .toCompletableFuture();
                            assertThatThrownBy(() -> stage.get(3, TimeUnit.SECONDS))
                                    .hasCauseInstanceOf(ApiException.class);
                        });
    }

    @Test
    void namespaceIsolationSharedModeAndOverrides() {
        context()
                .withPropertyValues("platform.redis.namespace-mode=SHARED")
                .run(ctx -> assertThat(ctx).hasFailed());
        context()
                .withPropertyValues(
                        "platform.redis.namespace-mode=SHARED", "platform.redis.namespace=team")
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            String key = key();
                            store.set(key, "value");
                            assertThat(ctx.getBean(StringRedisTemplate.class).hasKey("team:" + key))
                                    .isTrue();
                        });
        RedisStore custom = org.mockito.Mockito.mock(RedisStore.class);
        context()
                .withBean(RedisStore.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(RedisStore.class)).isSameAs(custom));
    }

    @Test
    void asyncMirrorsExpiryPersistenceNxAndDeletion() {
        context()
                .run(
                        ctx -> {
                            var async = ctx.getBean(AsyncRedisStore.class);
                            String key = key();
                            assertThat(async.ttl(key).toCompletableFuture().get()).isEmpty();
                            assertThat(
                                            async.setIfAbsent(key, "first", Duration.ofSeconds(3))
                                                    .toCompletableFuture()
                                                    .get())
                                    .isTrue();
                            assertThat(async.exists(key).toCompletableFuture().get()).isTrue();
                            assertThat(async.ttl(key).toCompletableFuture().get()).isPresent();
                            assertThat(async.setIfAbsent(key, "second").toCompletableFuture().get())
                                    .isFalse();
                            assertThat(async.persist(key).toCompletableFuture().get()).isTrue();
                            assertThat(async.ttl(key).toCompletableFuture().get()).isEmpty();
                            assertThat(
                                            async.expire(key, Duration.ofSeconds(3))
                                                    .toCompletableFuture()
                                                    .get())
                                    .isTrue();
                            async.set(key, "overwrite").toCompletableFuture().get();
                            assertThat(async.ttl(key).toCompletableFuture().get()).isEmpty();
                            async.set(key, "with-ttl", Duration.ofSeconds(3))
                                    .toCompletableFuture()
                                    .get();
                            assertThat(async.ttl(key).toCompletableFuture().get()).isPresent();
                            assertThat(async.delete(key).toCompletableFuture().get()).isTrue();
                            assertThat(async.exists(key).toCompletableFuture().get()).isFalse();
                        });
    }
}
