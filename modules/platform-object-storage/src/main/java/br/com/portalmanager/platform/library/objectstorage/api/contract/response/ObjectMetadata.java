package br.com.portalmanager.platform.library.objectstorage.api.contract.response;

import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;

import java.time.Instant;
import java.util.Map;

/** Metadata returned for an object. ETags are provider values and are not guaranteed to be content hashes. */
public record ObjectMetadata(
        ObjectReference reference,
        long sizeBytes,
        ObjectHeaders headers,
        String eTag,
        Instant lastModified,
        Map<String, String> metadata) {
    public ObjectMetadata {
        if (reference == null) throw ContractValidation.invalid("reference");
        if (sizeBytes < 0) throw ContractValidation.invalid("sizeBytes");
        if (metadata != null && metadata.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)) {
            throw ContractValidation.invalid("metadata");
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
