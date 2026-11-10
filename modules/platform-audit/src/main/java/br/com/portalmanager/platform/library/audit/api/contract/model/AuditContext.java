package br.com.portalmanager.platform.library.audit.api.contract.model;

public record AuditContext(
        String accountId,
        String applicationId,
        String environmentId,
        String actor,
        String correlationId
) {
}
