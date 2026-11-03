package br.com.portalmanager.platform.library.redis;

import br.com.portalmanager.platform.library.redis.store.RedisStore;
import br.com.portalmanager.platform.library.redis.store.RedisKeyPage;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class RedisScanIT extends RedisIntegrationSupport {
    @Test void pagesThroughOnlyLogicalKeysOfTheLiteralPrefixWithoutReadingValues() {
        context().run(ctx -> {
                    RedisStore store = ctx.getBean(RedisStore.class);
                    StringRedisTemplate raw = ctx.getBean(StringRedisTemplate.class);
                    String prefix = key() + ".";
                    Map<String, String> entries = new LinkedHashMap<>();
                    for (int index = 0; index < 73; index++) entries.put(prefix + index, "secret-value");
                    store.withNamespace("dynamic-parameter").setAll(entries);
                    store.withNamespace("dynamic-parameter").set(prefix.substring(0, prefix.length() - 1) + "-other.key", "foreign-service");
                    raw.opsForValue().set("foreign:" + prefix + "key", "secret-foreign");
                    raw.opsForValue().set("dynamic-parameter:" + prefix + "invalid-json", "not-json");
                    Set<String> keys = new LinkedHashSet<>();
                    String cursor = "0";
                    int pages = 0;
                    do {
                        RedisKeyPage page = store.withNamespace("dynamic-parameter").scan(prefix, cursor, 1);
                        keys.addAll(page.keys());
                        cursor = page.cursor();
                        assertThat(++pages).isLessThan(10000);
                    } while (!"0".equals(cursor));
                    assertThat(keys).containsAll(entries.keySet()).hasSize(74);
                    assertThat(keys).allMatch(name -> name.startsWith(prefix));
                    assertThat(pages).isGreaterThan(1);
                });
    }

    @Test void globCharactersInPrefixAreTreatedLiterally() {
        context().run(ctx -> {
            RedisStore store = ctx.getBean(RedisStore.class);
            String prefix = key() + "[*]?\\.";
            store.set(prefix + "literal", "value");
            store.set(prefix.replace("[*]?\\", "x") + "foreign", "value");
            Set<String> keys = new LinkedHashSet<>();
            String cursor = "0";
            do {
                RedisKeyPage page = store.scan(prefix, cursor, 10);
                keys.addAll(page.keys()); cursor = page.cursor();
            } while (!"0".equals(cursor));
            assertThat(keys).containsExactly(prefix + "literal");
        });
    }

    @Test void unavailableRedisIsAnErrorRatherThanAnEmptyPage() {
        context().withPropertyValues("platform.redis.connection.port=1", "platform.redis.connection.command-timeout=100ms",
                "platform.redis.connection.connect-timeout=100ms").run(ctx -> {
            RedisStore store = ctx.getBean(RedisStore.class);
            assertThatThrownBy(() -> store.scan("service.", "0", 10)).isInstanceOf(ApiException.class);
        });
    }
}
