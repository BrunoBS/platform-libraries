package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.operations.RedisOperations;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;

class RedisGuardedOperationsIT extends RedisIntegrationSupport {
    @Test
    void guardComparisonAndMutationAreAtomicAndDeleteIsIdempotent() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var atomic = ctx.getBean(RedisOperations.class).atomic();
                            store.set("guard", "a", Duration.ofSeconds(5));
                            store.set("target", 1, Duration.ofSeconds(5));
                            assertThat(atomic.setIfValue("guard", "b", "target", 2)).isFalse();
                            assertThat(atomic.deleteIfValue("guard", "b", "target")).isFalse();
                            assertThat(atomic.compareAndDelete("guard", "b")).isFalse();
                            assertThat(store.get("target", Integer.class)).contains(1);
                            assertThat(atomic.setIfValue("guard", "a", "target", 2)).isTrue();
                            assertThat(store.get("target", Integer.class)).contains(2);
                            assertThat(store.ttl("target")).isEmpty();
                            assertThat(atomic.deleteIfValue("guard", "a", "target")).isTrue();
                            assertThat(atomic.deleteIfValue("guard", "a", "target")).isTrue();
                            assertThat(atomic.compareAndDelete("guard", "a")).isTrue();
                            assertThat(store.exists("guard")).isFalse();
                            assertThatThrownBy(() -> atomic.setIfValue("same", "a", "same", 1))
                                    .hasMessage("redis.validation");
                        });
    }

    @Test
    void anExpiredGuardCannotMutateOrDeleteItsReplacement() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var atomic = ctx.getBean(RedisOperations.class).atomic();
                            store.set("guard", "a", Duration.ofMillis(50));
                            await().atMost(Duration.ofSeconds(3))
                                    .until(() -> !store.exists("guard"));
                            store.set("guard", "b");
                            store.set("target", 2);
                            assertThat(atomic.setIfValue("guard", "a", "target", 1)).isFalse();
                            assertThat(atomic.deleteIfValue("guard", "a", "target")).isFalse();
                            assertThat(atomic.compareAndDelete("guard", "a")).isFalse();
                            assertThat(store.get("target", Integer.class)).contains(2);
                            assertThat(store.get("guard", String.class)).contains("b");
                        });
    }

    @Test
    void scanIsLiteralNamespaceScopedAndDoesNotSwallowCallbackFailures() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var keys = ctx.getBean(RedisOperations.class).keys();
                            store.set("literal[*].one", 1);
                            store.set("literalX.two", 2);
                            ctx.getBean(StringRedisTemplate.class)
                                    .opsForValue()
                                    .set("other:literal[*].foreign", "3");
                            var found = new ArrayList<String>();
                            keys.scan("literal[*]", 1, found::add);
                            assertThat(found).containsExactly("literal[*].one");
                            found.clear();
                            keys.scan("", 1, found::add);
                            assertThat(found)
                                    .contains("literal[*].one", "literalX.two")
                                    .doesNotContain("literal[*].foreign");
                            var failure = new IllegalStateException("callback");
                            assertThatThrownBy(
                                            () ->
                                                    keys.scan(
                                                            "literal[*]",
                                                            1,
                                                            key -> {
                                                                throw failure;
                                                            }))
                                    .isSameAs(failure);
                            assertThatThrownBy(() -> keys.scan("", 0, found::add))
                                    .hasMessage("redis.validation");
                        });
    }
}
