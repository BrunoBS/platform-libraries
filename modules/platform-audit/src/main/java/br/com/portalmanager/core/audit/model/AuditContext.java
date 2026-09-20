package br.com.portalmanager.core.audit.model;

public record AuditContext(
        String accountId,
        String applicationId,
        String environmentId,
        String actor,
        String correlationId
) {
}
