package br.com.portalmanager.platform.library.redis.internal.common;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class RedisAccess {
    private final PlatformRedisProperties properties;
    private final String defaultNamespace;
    private final String prefix;

    public RedisAccess(PlatformRedisProperties properties, String applicationName) {
        this.properties = properties;
        if (!validNamespace(applicationName)) throw RedisErrors.configuration();
        defaultNamespace = applicationName;
        prefix = defaultPrefix(applicationName);
        if (prefix.getBytes(StandardCharsets.UTF_8).length >= properties.limits().maxKeyBytes())
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

    public String prefix(String namespace) {
        if (!validNamespace(namespace)) throw RedisErrors.invalid();
        String value = namespace + ":";
        if (value.getBytes(StandardCharsets.UTF_8).length >= properties.limits().maxKeyBytes())
            throw RedisErrors.invalid();
        return value;
    }

    public String key(String key) {
        return key(defaultNamespace, key);
    }

    /** Explicit namespace replaces the application-name default; it is never nested under it. */
    public String key(String namespace, String key) {
        text(key);
        return text(prefix(namespace) + key);
    }

    private String defaultPrefix(String namespace) {
        if (!validNamespace(namespace)) throw RedisErrors.configuration();
        return namespace + ":";
    }

    public String text(String text) {
        if (text == null
                || text.isBlank()
                || text.getBytes(StandardCharsets.UTF_8).length > properties.limits().maxKeyBytes()
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
        if (limit < 1 || limit > properties.limits().maxReadSize()) throw RedisErrors.invalid();
        return limit;
    }

    public void batch(int size) {
        if (size < 1 || size > properties.limits().maxBatchSize()) throw RedisErrors.invalid();
    }

    public void batch(String operation, int size, int maximum) {
        if (size < 1) throw RedisErrors.invalid();
        if (size > maximum) throw RedisErrors.batchLimit(operation, size, maximum);
    }

    public void range(long start, long end) {
        if (start < 0 || end < start || end - start >= properties.limits().maxReadSize())
            throw RedisErrors.invalid();
    }
}
