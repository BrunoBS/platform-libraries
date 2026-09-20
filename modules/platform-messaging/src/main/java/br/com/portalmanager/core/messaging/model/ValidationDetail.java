package br.com.portalmanager.core.messaging.model;

import java.util.Map;

public record ValidationDetail(
        String field,
        String messageKey,
        Map<String, Object> parameters,
        String defaultMessage
) {
    public ValidationDetail(String field, String messageKey) {
        this(field, messageKey, Map.of(), null);
    }

    public ValidationDetail(
            String field,
            String messageKey,
            Map<String, Object> parameters
    ) {
        this(field, messageKey, parameters, null);
    }

    public static ValidationDetail literal(String field, String message) {
        return new ValidationDetail(field, null, Map.of(), message);
    }

    public ValidationDetail {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
