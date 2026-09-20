package br.com.portalmanager.core.audit.model;

import java.time.Instant;
import java.util.Map;

public record AuditEventRequest(
        Instant timestamp,
        String service,
        String accountId,
        String applicationId,
        String environmentId,
        String resource,
        String resourceId,
        String action,
        String actor,
        String correlationId,
        Integer httpStatus,
        Object payload,
        Map<String, Object> metadata
) {
}
