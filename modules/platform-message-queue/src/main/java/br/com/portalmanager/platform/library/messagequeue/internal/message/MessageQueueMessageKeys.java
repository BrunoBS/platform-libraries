package br.com.portalmanager.platform.library.messagequeue.internal.message;

public final class MessageQueueMessageKeys {

    private static final String PREFIX = "messagequeue.";

    public static final String PUBLISH_FAILED = PREFIX + "publish.failed";
    public static final String PUBLISH_OPTIONS_UNSUPPORTED = PREFIX + "publish.options.unsupported";
    public static final String PUBLISH_OPTIONS_INVALID = PREFIX + "publish.options.invalid";
    public static final String PUBLISH_PAYLOAD_INVALID = PREFIX + "publish.payload.invalid";
    public static final String PUBLISHER_DISABLED = PREFIX + "publisher.disabled";
    public static final String SERIALIZATION_FAILED = PREFIX + "serialization.failed";
    public static final String DESERIALIZATION_FAILED = PREFIX + "deserialization.failed";
    public static final String LISTENER_INVOCATION_FAILED = PREFIX + "listener.invocation.failed";

    private MessageQueueMessageKeys() {
    }
}
