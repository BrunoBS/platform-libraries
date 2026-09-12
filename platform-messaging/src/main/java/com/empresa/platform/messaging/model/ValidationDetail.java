package com.empresa.platform.messaging.model;

import java.util.Map;

public record ValidationDetail(
        String field,
        String messageKey,
        Map<String, Object> parameters
) {
    public ValidationDetail(String field, String messageKey) {
        this(field, messageKey, Map.of());
    }

    public ValidationDetail {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
