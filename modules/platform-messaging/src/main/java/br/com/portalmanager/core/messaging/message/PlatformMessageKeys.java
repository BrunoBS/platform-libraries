package br.com.portalmanager.core.messaging.message;

public final class PlatformMessageKeys {

    private static final String PREFIX = "platform.";

    public static final String VALIDATION_FAILED = PREFIX + "global.validation.failed";
    public static final String REQUEST_NOT_READABLE = PREFIX + "global.request.readable";
    public static final String REQUEST_FORMAT_INVALID = PREFIX + "global.request.format";
    public static final String DATA_INTEGRITY = PREFIX + "global.data.integrity";
    public static final String RESOURCE_NOT_FOUND = PREFIX + "global.resource.not.found";
    public static final String INTERNAL_SERVER_ERROR = PREFIX + "global.internal.server.error";
    public static final String TYPE_MISMATCH = PREFIX + "global.validation.type.mismatch";
    public static final String INVALID_ENUM = PREFIX + "validation.invalid.enum";
    public static final String UNEXPECTED_ERROR = PREFIX + "global.unexpected.error";
    public static final String RESOURCE_VERSION_CONFLICT = PREFIX + "global.resource.version.conflict";

    private PlatformMessageKeys() {
    }
}
