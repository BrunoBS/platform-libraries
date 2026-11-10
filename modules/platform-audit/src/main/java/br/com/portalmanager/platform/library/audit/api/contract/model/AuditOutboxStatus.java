package br.com.portalmanager.platform.library.audit.api.contract.model;

/** Technical delivery state used by the audit outbox processor. */
public enum AuditOutboxStatus {
    PENDING,
    PROCESSING,
    RETRY,
    ERROR
}
