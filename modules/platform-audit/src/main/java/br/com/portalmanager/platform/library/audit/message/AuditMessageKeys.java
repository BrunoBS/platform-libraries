package br.com.portalmanager.platform.library.audit.message;

public final class AuditMessageKeys {

    private static final String PREFIX = "audit.";

    public static final String SERVICE_URL_REQUIRED = PREFIX + "service-url.required";
    public static final String USER_CONTEXT_MISSING = PREFIX + "context.user.missing";
    public static final String QUEUE_MISSING = PREFIX + "queue.missing";
    public static final String REDIS_NOT_CONFIGURED = PREFIX + "queue.redis.not-configured";
    public static final String QUEUE_PERSIST_FAILED = PREFIX + "queue.persist.failed";
    public static final String QUEUE_DESERIALIZE_FAILED = PREFIX + "queue.deserialize.failed";

    private AuditMessageKeys() {
    }
}
