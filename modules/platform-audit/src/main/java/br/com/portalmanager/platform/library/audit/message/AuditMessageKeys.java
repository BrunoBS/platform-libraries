package br.com.portalmanager.platform.library.audit.message;

public final class AuditMessageKeys {

    private static final String PREFIX = "audit.";

    public static final String MESSAGE_QUEUE_DESTINATION_REQUIRED = PREFIX + "destination.required";
    public static final String MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED = PREFIX + "destination.not-configured";
    public static final String USER_CONTEXT_MISSING = PREFIX + "context.user.missing";
    public static final String RESOURCE_IDENTIFIER_MISSING = PREFIX + "resource.identifier.missing";
    public static final String FIELD_NOT_ALLOWED = PREFIX + "field.not-allowed";
    public static final String FIELD_RESOLUTION_FAILED = PREFIX + "field.resolve-failed";
    public static final String RESOURCE_ACTION_REQUIRED = PREFIX + "resource-action.required";
    public static final String EVENT_TOO_LARGE = PREFIX + "event.too-large";
    public static final String EVENT_SERIALIZATION_FAILED = PREFIX + "event.serialization-failed";
    public static final String EVENT_COUNT_EXCEEDED = PREFIX + "events-per-invocation.exceeded";

    private AuditMessageKeys() {
    }
}