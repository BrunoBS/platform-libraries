package br.com.portalmanager.platform.library.objectstorage.contract;

import java.time.Instant;
import java.util.Map;

public record ObjectMetadata(
        ObjectReference reference,
        long sizeBytes,
        String contentType,
        String eTag,
        Instant lastModified,
        Map<String, String> metadata) {
    public ObjectMetadata {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
