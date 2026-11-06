package br.com.portalmanager.platform.library.objectstorage.api.contract;


/** Logical reference to an object. A missing versionId means the current object. */
public record ObjectReference(String destination, String key, String versionId) {
    public ObjectReference {
        if (destination == null || destination.isBlank()) throw ContractValidation.invalid("destination");
        if (key == null || key.isBlank()) throw ContractValidation.invalid("key");
        if (versionId != null && versionId.isBlank()) throw ContractValidation.invalid("versionId");
    }

    public static ObjectReference current(String destination, String key) {
        return new ObjectReference(destination, key, null);
    }
}
