package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Context-owned stores; no global mutation and no additional bean candidates or connections. */
public final class RedisNamespaceStores {
    private final ConfigurableListableBeanFactory beans;
    private final Environment environment;
    private final Map<Scope, RedisStore> stores = new HashMap<>();

    public RedisNamespaceStores(ConfigurableListableBeanFactory beans, Environment environment) {
        this.beans = beans;
        this.environment = environment;
    }

    public synchronized RedisStore resolve(RedisNamespace annotation) {
        if (!environment.getProperty("platform.redis.enabled", Boolean.class, true)
                || beans.getBeanNamesForType(PlatformRedisProperties.class, true, false).length == 0
                || beans.getBeanNamesForType(StringRedisTemplate.class, true, false).length == 0
                || beans.getBeanNamesForType(RedisJsonCodec.class, true, false).length == 0) {
            throw RedisErrors.configuration();
        }
        Scope scope = new Scope(annotation.value(), annotation.mode());
        return stores.computeIfAbsent(scope, this::create);
    }

    private RedisStore create(Scope scope) {
        PlatformRedisProperties defaults = beans.getBean(PlatformRedisProperties.class);
        PlatformRedisProperties properties = new PlatformRedisProperties(
                scope.namespace(),
                scope.mode(),
                defaults.maxKeyBytes(),
                defaults.maxPayloadBytes(),
                defaults.maxBatchSize(),
                defaults.maxReadSize(),
                defaults.listenerThreads(),
                defaults.listenerQueueCapacity(),
                defaults.maxStreamWorkers(),
                defaults.streamMaxLength(),
                defaults.shutdownTimeout(),
                defaults.reconnectMin(),
                defaults.reconnectMax(),
                defaults.keyspaceEnabled());
        RedisAccess access = new RedisAccess(properties, environment.getProperty("spring.application.name"));
        return new DefaultRedisStore(
                beans.getBean(StringRedisTemplate.class),
                beans.getBean(RedisJsonCodec.class),
                access);
    }

    private record Scope(String namespace, NamespaceMode mode) {}
}
