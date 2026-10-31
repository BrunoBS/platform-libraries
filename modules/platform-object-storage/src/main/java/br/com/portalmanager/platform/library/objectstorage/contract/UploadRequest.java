package br.com.portalmanager.platform.library.objectstorage.contract;

import java.util.Map;
import java.util.Objects;

public record UploadRequest(
        String destination,
        String key,
        long contentLength,
        String contentType,
        Map<String, String> metadata,
        WriteMode writeMode) {

    public UploadRequest {
        Objects.requireNonNull(destination, "destination is required");
        Objects.requireNonNull(key, "key is required");
        Objects.requireNonNull(writeMode, "writeMode is required");
        if (destination.isBlank() || key.isBlank() || contentLength < 0) {
            throw new IllegalArgumentException("destination/key must not be blank and contentLength must be non-negative");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public UploadRequest(String destination, String key, long contentLength, String contentType, Map<String, String> metadata) {
        this(destination, key, contentLength, contentType, metadata, WriteMode.CREATE_ONLY);
    }
}
