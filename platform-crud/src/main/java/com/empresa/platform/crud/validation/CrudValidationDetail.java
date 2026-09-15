package com.empresa.platform.crud.validation;

import java.util.Map;

public record CrudValidationDetail(
        String field,
        String messageKey,
        Map<String, Object> parameters,
        String defaultMessage
) {

    public CrudValidationDetail(String field, String messageKey) {
        this(field, messageKey, Map.of(), null);
    }

    public CrudValidationDetail(
            String field,
            String messageKey,
            Map<String, Object> parameters
    ) {
        this(field, messageKey, parameters, null);
    }

    public static CrudValidationDetail literal(String field, String message) {
        return new CrudValidationDetail(field, null, Map.of(), message);
    }

    public CrudValidationDetail {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
