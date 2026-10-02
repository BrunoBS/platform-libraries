package br.com.portalmanager.platform.library.messaging.model;

import java.util.Map;

public record ValidationDetail(
        String field,
        String messageKey,
        Map<String, Object> parameters,
        String defaultMessage,
        String fallbackMessageKey
) {
    public ValidationDetail(String field, String messageKey) {
        this(field, messageKey, Map.of(), null, null);
    }

    public ValidationDetail(
            String field,
            String messageKey,
            Map<String, Object> parameters
    ) {
        this(field, messageKey, parameters, null, null);
    }

    public static ValidationDetail literal(String field, String message) {
        return new ValidationDetail(field, null, Map.of(), message, null);
    }

    public ValidationDetail(
            String field,
            String messageKey,
            Map<String, Object> parameters,
            String fallbackMessageKey
    ) {
        this(field, messageKey, parameters, null, fallbackMessageKey);
    }

    public ValidationDetail {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
