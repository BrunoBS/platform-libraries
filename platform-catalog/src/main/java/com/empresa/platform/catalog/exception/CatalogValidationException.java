package com.empresa.platform.catalog.exception;

import com.empresa.platform.messaging.model.ValidationDetail;

import java.util.List;
import java.util.Map;

public class CatalogValidationException extends CatalogException {

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
