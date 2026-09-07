package com.empresa.platform.messaging.model;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        String solution,
        Instant timestamp,
        String path
        ) {
}
