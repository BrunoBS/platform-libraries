package br.com.portalmanager.platform.library.schemavalidation.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.schema-validation")
public class PlatformSchemaValidationProperties {

    public static final String DEFAULT_FALLBACK_CODE = "DEFAULT";
    public static final String DEFAULT_VIEW_NAME = "vw_platform_resource_schemas";
    public static final Duration DEFAULT_REDIS_TTL = Duration.ofHours(6);
    public static final Duration DEFAULT_LOCAL_TTL = Duration.ofMinutes(15);
    public static final long DEFAULT_LOCAL_MAX_SIZE = 500;

    private String fallbackCode = DEFAULT_FALLBACK_CODE;
    private String viewName = DEFAULT_VIEW_NAME;
    private final Cache cache = new Cache();

    public String getFallbackCode() {
        return fallbackCode;
    }

    public void setFallbackCode(String fallbackCode) {
        this.fallbackCode = fallbackCode;
    }

    public String resolveFallbackCode() {
        return fallbackCode == null || fallbackCode.isBlank()
                ? DEFAULT_FALLBACK_CODE
                : fallbackCode.trim();
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String resolveViewName() {
        return viewName == null || viewName.isBlank()
                ? DEFAULT_VIEW_NAME
                : viewName.trim();
    }
    public Cache getCache() {
        return cache;
    }

    public static class Cache {
        private final Redis redis = new Redis();
        private final Local local = new Local();

        public Redis getRedis() { return redis; }
        public Local getLocal() { return local; }
    }

    public static class Redis {
        private boolean enabled;
        private Duration ttl = DEFAULT_REDIS_TTL;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = requirePositive(ttl, "platform.schema-validation.cache.redis.ttl"); }
    }

    private static Duration requirePositive(Duration value, String property) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(property + " must be greater than zero");
        }
        return value;
    }

    public static class Local {
        private boolean enabled = true;
        private Duration ttl = DEFAULT_LOCAL_TTL;
        private long maxSize = DEFAULT_LOCAL_MAX_SIZE;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = requirePositive(ttl, "platform.schema-validation.cache.local.ttl"); }
        public long getMaxSize() { return maxSize; }
        public void setMaxSize(long maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("platform.schema-validation.cache.local.max-size must be greater than zero");
            }
            this.maxSize = maxSize;
        }
    }
}

