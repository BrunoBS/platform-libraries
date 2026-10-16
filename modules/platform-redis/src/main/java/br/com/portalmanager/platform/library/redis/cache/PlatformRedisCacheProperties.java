package br.com.portalmanager.platform.library.redis.cache;

import br.com.portalmanager.platform.library.redis.internal.RedisErrors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties("platform.redis.cache")
public record PlatformRedisCacheProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("PT30M") Duration defaultTtl,
        Map<String, Region> regions,
        @DefaultValue("") String environment,
        @DefaultValue("FAIL_FAST") FailurePolicy failurePolicy,
        @DefaultValue("PT5S") Duration lockTtl) {
    public enum FailurePolicy {
        FAIL_FAST,
        FAIL_OPEN_READ
    }

    public record Region(Duration ttl) {
        public Region {
            positive(ttl);
        }
    }

    public PlatformRedisCacheProperties {
        positive(defaultTtl);
        positive(lockTtl);
        if (failurePolicy == null) throw RedisErrors.configuration();
        regions = regions == null ? Map.of() : Map.copyOf(regions);
        for (String region : regions.keySet()) name(region);
    }

    public Duration ttl(String name) {
        return regions.containsKey(name) ? regions.get(name).ttl() : defaultTtl;
    }

    static void positive(Duration ttl) {
        try {
            if (ttl == null || ttl.toMillis() < 1) throw RedisErrors.configuration();
        } catch (ArithmeticException failure) {
            throw RedisErrors.configuration();
        }
    }

    static void name(String name) {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
            throw RedisErrors.configuration();
    }
}
