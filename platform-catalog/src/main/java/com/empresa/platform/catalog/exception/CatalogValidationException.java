package com.empresa.platform.catalog.exception;

import com.empresa.platform.crud.validation.CrudValidationDetail;

import java.util.List;
import java.util.Map;

public class CatalogValidationException extends CatalogException {

    private final List<CrudValidationDetail> details;

    public CatalogValidationException(
            String code,
            List<CrudValidationDetail> details,
            Map<String, Object> parameters) {
        super(code, parameters);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<CrudValidationDetail> getDetails() {
        return details;
    }
}
