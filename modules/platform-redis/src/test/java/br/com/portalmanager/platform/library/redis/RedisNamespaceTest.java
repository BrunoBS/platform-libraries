package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.redis.api.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.redis.internal.common.RedisAccess;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import java.util.Map;

class RedisNamespaceTest {
    private final PlatformRedisProperties properties = new Binder(
            new MapConfigurationPropertySource(Map.of()))
            .bindOrCreate("platform.redis", PlatformRedisProperties.class);

    @Test
    void applicationNameIsTheDefaultAndExplicitNamespaceReplacesIt() {
        RedisAccess access = new RedisAccess(properties, "orders-service");

        assertThat(access.key("orders.42")).isEqualTo("orders-service:orders.42");
        assertThat(access.key("dynamic-parameter", "orders.42"))
                .isEqualTo("dynamic-parameter:orders.42");
        assertThat(access.key("dynamic-parameter", "orders.42"))
                .doesNotStartWith("orders-service:");
    }

    @Test
    void requiresApplicationNameAndValidExplicitNamespace() {
        assertThatThrownBy(() -> new RedisAccess(properties, ""))
                .isInstanceOf(PlatformConfigurationException.class);
        RedisAccess access = new RedisAccess(properties, "orders-service");
        assertThatThrownBy(() -> access.key("bad:namespace", "orders.42"))
                .isInstanceOf(ValidationException.class);
    }
}
