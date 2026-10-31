package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;

/** Logical reference to an object. A missing versionId means the current object. */
public record ObjectReference(String destination, String key, String versionId) {
    public ObjectReference {
        if (destination == null || destination.isBlank()) throw ObjectStorageValidation.invalid("destination");
        if (key == null || key.isBlank()) throw ObjectStorageValidation.invalid("key");
        if (versionId != null && versionId.isBlank()) throw ObjectStorageValidation.invalid("versionId");
    }

    public static ObjectReference current(String destination, String key) {
        return new ObjectReference(destination, key, null);
    }
}
