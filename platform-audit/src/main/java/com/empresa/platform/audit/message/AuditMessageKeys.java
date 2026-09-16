package com.empresa.platform.audit.message;

public final class AuditMessageKeys {

    private static final String PREFIX = "audit.";

    public static final String SERVICE_URL_REQUIRED = PREFIX + "service-url.required";
    public static final String USER_CONTEXT_MISSING = PREFIX + "context.user.missing";
    public static final String FALLBACK_STORE_MISSING = PREFIX + "fallback.store.missing";
    public static final String REDIS_NOT_CONFIGURED = PREFIX + "fallback.redis.not-configured";
    public static final String FALLBACK_PERSIST_FAILED = PREFIX + "fallback.persist.failed";
    public static final String FALLBACK_DESERIALIZE_FAILED = PREFIX + "fallback.deserialize.failed";

    private AuditMessageKeys() {
    }
}
