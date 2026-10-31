package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.time.Instant;
import java.util.Map;

public record ObjectMetadata(
        ObjectReference reference,
        long sizeBytes,
        ObjectHeaders headers,
        String eTag,
        Instant lastModified,
        Map<String, String> metadata) {
    public ObjectMetadata {
        if (reference == null) throw ObjectStorageValidation.invalid("reference");
        if (sizeBytes < 0) throw ObjectStorageValidation.invalid("sizeBytes");
        if (metadata != null && metadata.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)) {
            throw ObjectStorageValidation.invalid("metadata");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        headers = headers == null ? ObjectHeaders.empty() : headers;
    }

    public ObjectMetadata(
            ObjectReference reference,
            long sizeBytes,
            String contentType,
            String eTag,
            Instant lastModified,
            Map<String, String> metadata) {
        this(reference, sizeBytes, new ObjectHeaders(contentType, null, null, null, null), eTag, lastModified, metadata);
    }

    public String contentType() {
        return headers.contentType();
    }
}
