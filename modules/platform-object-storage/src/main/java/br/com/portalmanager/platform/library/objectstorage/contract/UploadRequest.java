package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.util.Map;

public record UploadRequest(
        String destination,
        String key,
        long contentLength,
        ObjectHeaders headers,
        Map<String, String> metadata,
        WriteMode writeMode) {

    public UploadRequest {
        if (destination == null || destination.isBlank()) throw ObjectStorageValidation.invalid("destination");
        if (key == null || key.isBlank()) throw ObjectStorageValidation.invalid("key");
        if (writeMode == null) throw ObjectStorageValidation.invalid("writeMode");
        if (contentLength < 0) throw ObjectStorageValidation.invalid("contentLength");
        headers = headers == null ? ObjectHeaders.empty() : headers;
        if (metadata != null && metadata.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)) {
            throw ObjectStorageValidation.invalid("metadata");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public UploadRequest(String destination, String key, long contentLength, String contentType, Map<String, String> metadata) {
        this(destination, key, contentLength, new ObjectHeaders(contentType, null, null, null, null), metadata, WriteMode.CREATE_ONLY);
    }

    public UploadRequest(String destination, String key, long contentLength, String contentType, Map<String, String> metadata, WriteMode writeMode) {
        this(destination, key, contentLength, new ObjectHeaders(contentType, null, null, null, null), metadata, writeMode);
    }

    public String contentType() {
        return headers.contentType();
    }
}
