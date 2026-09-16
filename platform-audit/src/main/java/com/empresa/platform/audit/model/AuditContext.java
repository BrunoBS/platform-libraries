package com.empresa.platform.audit.model;

public record AuditContext(
        String accountId,
        String applicationId,
        String environmentId,
        String actor,
        String correlationId
) {
}
