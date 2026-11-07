package br.com.portalmanager.platform.library.objectstorage.api.contract.request;

import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.WriteMode;

import java.util.Map;

/**
 * Upload metadata and write policy. {@code contentLength} is the exact number of bytes the supplied stream contains;
 * the caller is responsible for providing a matching length.
 */
public record UploadRequest(
        String destination,
        String key,
        long contentLength,
        ObjectHeaders headers,
        Map<String, String> metadata,
        WriteMode writeMode) {

    public UploadRequest {
        if (destination == null || destination.isBlank()) throw ContractValidation.invalid("destination");
        if (key == null || key.isBlank()) throw ContractValidation.invalid("key");
        if (writeMode == null) throw ContractValidation.invalid("writeMode");
        if (contentLength < 0) throw ContractValidation.invalid("contentLength");
        headers = headers == null ? ObjectHeaders.empty() : headers;
        if (metadata != null && metadata.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)) {
            throw ContractValidation.invalid("metadata");
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
