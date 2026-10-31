package br.com.portalmanager.platform.library.objectstorage.exception;

public class ObjectStorageException extends RuntimeException {
    public enum Reason { NOT_FOUND, ALREADY_EXISTS, ACCESS_DENIED, TRANSIENT, PROVIDER_FAILURE }
    private final Reason reason;

    public ObjectStorageException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
