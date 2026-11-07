package br.com.portalmanager.platform.library.objectstorage.api.contract.response;

import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;

/**
 * Result of a completed upload. {@code eTag} is provider metadata and is not a portable content checksum;
 * {@code sha256} is computed from the bytes consumed by the library.
 */
public record UploadResult(ObjectReference reference, String eTag, long sizeBytes, String sha256) {
    public UploadResult(ObjectReference reference, String eTag, long sizeBytes) {
        this(reference, eTag, sizeBytes, null);
    }
}
