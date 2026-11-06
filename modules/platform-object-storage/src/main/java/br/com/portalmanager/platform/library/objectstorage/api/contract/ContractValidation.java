package br.com.portalmanager.platform.library.objectstorage.api.contract;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.objectstorage.message.ObjectStorageMessageKeys;
import java.util.Map;

/** Validation shared by the public contract value types. */
final class ContractValidation {
    private ContractValidation() { }

    static ValidationException invalid(String field) {
        return new ValidationException(ObjectStorageMessageKeys.INVALID_REQUEST, Map.of("0", field));
    }

    static void requireReference(ObjectReference reference) {
        if (reference == null) throw invalid("reference");
    }
}
