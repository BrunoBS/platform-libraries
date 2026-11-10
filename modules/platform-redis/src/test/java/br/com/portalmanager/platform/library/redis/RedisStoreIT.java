package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.api.exception.*;
import br.com.portalmanager.platform.library.redis.api.store.*;
import br.com.portalmanager.platform.library.redis.api.operations.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

class RedisStoreIT extends RedisIntegrationSupport {
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
    void applicationNameIsDefaultAndExplicitNamespaceOverridesIt() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var raw = ctx.getBean(StringRedisTemplate.class);
                            String key = key();
                            store.set(key, "value");
                            store.withNamespace("team").set(key, "shared-value");
                            assertThat(raw.hasKey("redis-test:" + key))
                                    .isTrue();
                            assertThat(raw.hasKey("team:" + key)).isTrue();
                            assertThat(raw.hasKey("redis-test:team:" + key)).isFalse();
                            assertThat(store.get(key, String.class)).contains("value");
                            assertThat(store.withNamespace("team").get(key, String.class)).contains("shared-value");
                        });
        RedisStore custom = org.mockito.Mockito.mock(RedisStore.class);
        context()
                .withBean(RedisStore.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(RedisStore.class)).isSameAs(custom));
    }

}
