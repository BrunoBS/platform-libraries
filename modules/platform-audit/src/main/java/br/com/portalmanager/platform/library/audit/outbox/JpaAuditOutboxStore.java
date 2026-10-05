package br.com.portalmanager.platform.library.audit.outbox;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;

public final class JpaAuditOutboxStore implements AuditOutboxStore {

    private final EntityManager entityManager;

    public JpaAuditOutboxStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void append(JsonNode payload, JsonNode metadata) {
        entityManager.persist(new AuditOutboxEntry(payload, metadata));
    }
}
