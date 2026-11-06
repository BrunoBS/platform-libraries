package br.com.portalmanager.platform.library.objectstorage.api.contract;

public record UploadResult(ObjectReference reference, String eTag, long sizeBytes, String sha256) {
    public UploadResult(ObjectReference reference, String eTag, long sizeBytes) {
        this(reference, eTag, sizeBytes, null);
    }
}
