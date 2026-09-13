package com.empresa.platform.crud.validation;

import java.util.Map;

public record CrudValidationDetail(
        String field,
        String messageKey,
        Map<String, Object> parameters
) {

    public CrudValidationDetail(String field, String messageKey) {
        this(field, messageKey, Map.of());
    }

    public CrudValidationDetail {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
