package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class RedisAccess {
    private final PlatformRedisProperties properties;
    private final String prefix;

    public RedisAccess(PlatformRedisProperties properties, String applicationName) {
        this.properties = properties;
        String namespace = properties.namespace();
        if (properties.namespaceMode() == PlatformRedisProperties.NamespaceMode.ISOLATED) {
            if (!validNamespace(applicationName)) throw RedisErrors.configuration();
            namespace =
                    namespace == null || namespace.isBlank()
                            ? applicationName
                            : namespace + "." + applicationName;
        } else if (namespace == null || namespace.isBlank()) {
            throw RedisErrors.configuration();
        }
        if (!validNamespace(namespace)) throw RedisErrors.configuration();
        prefix = namespace + ":";
        if (prefix.getBytes(StandardCharsets.UTF_8).length >= properties.maxKeyBytes())
            throw RedisErrors.configuration();
    }

    private boolean validNamespace(String value) {
        return value != null && value.matches("[A-Za-z0-9][A-Za-z0-9._-]*");
    }

    public PlatformRedisProperties properties() {
        return properties;
    }

    public String prefix() {
        return prefix;
    }

    public String key(String key) {
        text(key);
        return text(prefix + key);
    }

    public String text(String text) {
        if (text == null
                || text.isBlank()
                || text.getBytes(StandardCharsets.UTF_8).length > properties.maxKeyBytes()
                || text.chars().anyMatch(Character::isISOControl)) throw RedisErrors.invalid();
        return text;
    }

    public Duration duration(Duration duration) {
        if (duration == null || duration.compareTo(Duration.ofMillis(1)) < 0)
            throw RedisErrors.invalid();
        try {
            duration.toMillis();
        } catch (ArithmeticException failure) {
            throw RedisErrors.invalid();
        }
        return duration;
    }

    public int limit(int limit) {
        if (limit < 1 || limit > properties.maxReadSize()) throw RedisErrors.invalid();
        return limit;
    }

    public void batch(int size) {
        if (size < 1 || size > properties.maxBatchSize()) throw RedisErrors.invalid();
    }

    public void range(long start, long end) {
        if (start < 0 || end < start || end - start >= properties.maxReadSize())
            throw RedisErrors.invalid();
    }
}
