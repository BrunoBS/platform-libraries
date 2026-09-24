package br.com.portalmanager.platform.catalog.exception;

import br.com.portalmanager.platform.messaging.exception.ValidationDetailsProvider;
import br.com.portalmanager.platform.messaging.model.ValidationDetail;

import java.util.List;
import java.util.Map;

public class CatalogValidationException extends CatalogException implements ValidationDetailsProvider {

    private final List<ValidationDetail> details;

    public CatalogValidationException(
            String code,
            List<ValidationDetail> details,
            Map<String, Object> parameters) {
        super(code, parameters);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ValidationDetail> getDetails() {
        return details;
    }
}
