package br.com.portalmanager.platform.library.objectstorage.api.contract.request;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import java.util.Map;

final class ContractValidation {
    private ContractValidation() { }

    static ValidationException invalid(String field) {
        return new ValidationException("objectstorage.request.invalid", Map.of("0", field));
    }

    static void requireReference(ObjectReference reference) {
        if (reference == null) throw invalid("reference");
    }
}
