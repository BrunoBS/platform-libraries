package br.com.portalmanager.platform.library.objectstorage.exception;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.message.ObjectStorageMessageKeys;
import java.util.Map;

public final class ObjectStorageValidation {
    private ObjectStorageValidation() { }

    public static ValidationException invalid(String field) {
        return new ValidationException(ObjectStorageMessageKeys.INVALID_REQUEST, Map.of("0", field));
    }

    public static void requireReference(ObjectReference reference) {
        if (reference == null) throw invalid("reference");
    }

    public static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw invalid(field);
    }
}
