package br.com.portalmanager.platform.library.objectstorage.api.contract.response;

import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import java.util.Map;

final class ContractValidation {
    private ContractValidation() { }

    static ValidationException invalid(String field) {
        return new ValidationException("objectstorage.request.invalid", Map.of("0", field));
    }
}
