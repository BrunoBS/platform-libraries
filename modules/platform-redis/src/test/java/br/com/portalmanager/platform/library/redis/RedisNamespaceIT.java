package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.internal.RedisAccess;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisNamespaceIT extends RedisIntegrationSupport {
    record Stores(RedisStore standard, RedisStore shared, RedisStore session) {}
    @Configuration(proxyBeanMethods=false)
    static class Consumer {
        @Bean Stores stores(RedisStore standard,
                @RedisNamespace(value="dynamic-parameter", mode=NamespaceMode.SHARED) RedisStore shared,
                @RedisNamespace("session") RedisStore session) {
            return new Stores(standard, shared, session);
        }
    }

    @Test void threeNamespacesUseSameInfrastructureWithoutCollisionsOrGlobalMutation() {
        context().withUserConfiguration(Consumer.class).run(ctx -> {
            Stores stores = ctx.getBean(Stores.class);
            RedisAccess access = ctx.getBean(RedisAccess.class);
            String prefix = access.prefix();
            String key = key();
            stores.standard().set(key, new Event("default", 1));
            stores.shared().set(key, new Event("shared", 2));
            stores.session().set(key, new Event("session", 3));
            assertThat(stores.standard().get(key, Event.class)).contains(new Event("default",1));
            assertThat(stores.shared().get(key, Event.class)).contains(new Event("shared",2));
            assertThat(stores.session().get(key, Event.class)).contains(new Event("session",3));
            StringRedisTemplate template = ctx.getBean(StringRedisTemplate.class);
            assertThat(template.hasKey("redis-test:"+key)).isTrue();
            assertThat(template.hasKey("dynamic-parameter:"+key)).isTrue();
            assertThat(template.hasKey("session.redis-test:"+key)).isTrue();
            assertThat(template.hasKey("dynamic-parameter:dynamic-parameter:"+key)).isFalse();
            assertThat(access.prefix()).isEqualTo(prefix);
            assertThat(ctx.getBeansOfType(StringRedisTemplate.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(RedisStore.class)).hasSize(1);
            assertThat(ctx.getBeansOfType(br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec.class)).hasSize(1);
        });
    }

    @Test void batchSerializationTtlOverwriteDeleteAndKeyLimitsRemainEnforced() {
        context().withUserConfiguration(Consumer.class).run(ctx -> {
            RedisStore store = ctx.getBean(Stores.class).shared();
            String one = key();
            String two = key();
            store.setAll(Map.of(one, new Event("one",1), two,new Event("two",2)));
            assertThat(store.getAll(List.of(one,two), Event.class)).containsAllEntriesOf(Map.of(one,new Event("one",1),two,new Event("two",2)));
            store.set(one,new Event("expires",3),Duration.ofMillis(150));
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertThat(store.get(one,Event.class)).isEmpty());
            store.set(two,new Event("overwrite",4));
            assertThat(store.get(two,Event.class)).contains(new Event("overwrite",4));
            store.delete(two);
            assertThat(store.get(two,Event.class)).isEmpty();
            assertThatThrownBy(() -> store.set("x".repeat(256),1)).isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.ValidationException.class);
            assertThat(ctx.getBean(Stores.class).standard().get(one,Event.class)).isEmpty();
        });
    }
}
