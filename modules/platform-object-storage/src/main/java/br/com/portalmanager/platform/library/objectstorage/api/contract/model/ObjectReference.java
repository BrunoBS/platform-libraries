package br.com.portalmanager.platform.library.objectstorage.api.contract.model;

import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import java.util.Map;


/** Logical reference to an object. A missing versionId means the current object. */
public record ObjectReference(String destination, String key, String versionId) {
    public ObjectReference {
        if (destination == null || destination.isBlank()) throw invalid("destination");
        if (key == null || key.isBlank()) throw invalid("key");
        if (versionId != null && versionId.isBlank()) throw invalid("versionId");
    }

    private static ValidationException invalid(String field) {
        return new ValidationException("objectstorage.request.invalid", Map.of("0", field));
    }

    public static ObjectReference current(String destination, String key) {
        return new ObjectReference(destination, key, null);
    }
}
