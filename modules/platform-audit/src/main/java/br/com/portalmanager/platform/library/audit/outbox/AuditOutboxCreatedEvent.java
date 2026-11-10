package br.com.portalmanager.platform.library.audit.outbox;

/** Identifies an outbox row committed by the application transaction. */
public record AuditOutboxCreatedEvent(String identifier) { }
