package br.com.portalmanager.platform.library.audit.model;

/** Technical delivery state for the future outbox worker. */
public enum AuditOutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    RETRY,
    FAILED
}
