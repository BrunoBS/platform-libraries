package com.empresa.platform.messaging.model;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        String solution,
        List<ApiValidationDetail> details,
        Instant timestamp,
        String path,
        String correlationId
) {
    public ApiErrorResponse(
            String code,
            String message,
            String solution,
            Instant timestamp,
            String path,
            String correlationId
    ) {
        this(code, message, solution, List.of(), timestamp, path, correlationId);
    }

    public ApiErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
