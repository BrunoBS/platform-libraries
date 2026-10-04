package br.com.portalmanager.platform.library.audit.message;

public final class AuditMessageKeys {

    private static final String PREFIX = "audit.";

    public static final String MESSAGE_QUEUE_DESTINATION_REQUIRED = PREFIX + "destination.required";
    public static final String MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED = PREFIX + "destination.not-configured";
    public static final String USER_CONTEXT_MISSING = PREFIX + "context.user.missing";

    private AuditMessageKeys() {
    }
}
