package br.com.portalmanager.platform.library.audit.message;

public final class AuditMessageKeys {

    private static final String PREFIX = "audit.";

    public static final String USER_CONTEXT_MISSING = PREFIX + "context.user.missing";
    public static final String RESOURCE_IDENTIFIER_MISSING = PREFIX + "resource.identifier.missing";
    public static final String EVENT_DEFINITION_REQUIRED = PREFIX + "event-definition.required";
    public static final String SNAPSHOT_REQUIRED = PREFIX + "snapshot.required";
    public static final String EVENT_SERIALIZATION_FAILED = PREFIX + "event.serialization-failed";
    public static final String PURGE_RETURN_REQUIRED = PREFIX + "purge.return.required";
    public static final String PURGE_SNAPSHOT_REQUIRED = PREFIX + "purge.snapshot.required";
    public static final String TRANSACTION_REQUIRED = PREFIX + "transaction.required";

    private AuditMessageKeys() {
    }
}
