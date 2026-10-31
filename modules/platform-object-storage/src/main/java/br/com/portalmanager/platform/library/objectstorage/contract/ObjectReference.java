package br.com.portalmanager.platform.library.objectstorage.contract;

import java.util.Objects;

/** Logical reference to an object. A missing versionId means the current object. */
public record ObjectReference(String destination, String key, String versionId) {
    public ObjectReference {
        Objects.requireNonNull(destination, "destination is required");
        Objects.requireNonNull(key, "key is required");
        if (destination.isBlank() || key.isBlank()) {
            throw new IllegalArgumentException("destination and key must not be blank");
        }
    }

    public static ObjectReference current(String destination, String key) {
        return new ObjectReference(destination, key, null);
    }
}
