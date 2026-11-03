package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.redis.core.RedisKeyPage;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisNamespaceIT extends RedisIntegrationSupport {
    @Test
    void explicitNamespaceReplacesApplicationNameAcrossStoreOperations() {
        context().run(ctx -> {
            RedisStore store = ctx.getBean(RedisStore.class);
            StringRedisTemplate raw = ctx.getBean(StringRedisTemplate.class);
            String key = key();

            store.set(key, "application");
            store.withNamespace("dynamic-parameter").set(key, "custom");
            store.withNamespace("dynamic-parameter").setAll(Map.of(key + ".batch", "batch"));

            assertThat(store.get(key, String.class)).contains("application");
            assertThat(store.withNamespace("dynamic-parameter").get(key, String.class)).contains("custom");
            assertThat(store.withNamespace("dynamic-parameter").get(key + ".batch", String.class))
                    .contains("batch");
            assertThat(raw.hasKey("redis-test:" + key)).isTrue();
            assertThat(raw.hasKey("dynamic-parameter:" + key)).isTrue();
            assertThat(raw.hasKey("redis-test:dynamic-parameter:" + key)).isFalse();

            String cursor = "0";
            java.util.Set<String> scanned = new java.util.LinkedHashSet<>();
            do {
                RedisKeyPage page = store.withNamespace("dynamic-parameter").scan(key, cursor, 10);
                scanned.addAll(page.keys());
                cursor = page.cursor();
            } while (!"0".equals(cursor));
            assertThat(scanned).contains(key, key + ".batch");

            assertThat(store.withNamespace("dynamic-parameter").deleteAll(List.of(key, key + ".batch")))
                    .isEqualTo(2);
            assertThat(store.exists(key)).isTrue();
        });
    }
}
