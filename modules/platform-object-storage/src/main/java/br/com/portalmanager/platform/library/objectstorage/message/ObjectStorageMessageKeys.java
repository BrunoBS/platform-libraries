package br.com.portalmanager.platform.library.objectstorage.message;

public final class ObjectStorageMessageKeys {
    private static final String PREFIX = "objectstorage.";

    public static final String OBJECT_NOT_FOUND = PREFIX + "object.not-found";
    public static final String OBJECT_ALREADY_EXISTS = PREFIX + "object.already-exists";
    public static final String ACCESS_DENIED = PREFIX + "access.denied";
    public static final String UNAUTHORIZED = PREFIX + "unauthorized";
    public static final String OPERATION_TIMEOUT = PREFIX + "operation.timeout";
    public static final String TRANSIENT_FAILURE = PREFIX + "operation.transient-failure";
    public static final String OPERATION_FAILED = PREFIX + "operation.failed";
    public static final String INVALID_REQUEST = PREFIX + "request.invalid";

    private ObjectStorageMessageKeys() { }
}
