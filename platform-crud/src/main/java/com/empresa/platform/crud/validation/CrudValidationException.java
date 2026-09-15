package com.empresa.platform.crud.validation;

import java.util.List;

public class CrudValidationException extends RuntimeException {

    private final List<CrudValidationDetail> details;

    public CrudValidationException(CrudValidationResult result) {
        super("CRUD validation failed");
        this.details = result == null
                ? List.of()
                : result.getDetails();
    }

    public List<CrudValidationDetail> getDetails() {
        return details;
    }
}
