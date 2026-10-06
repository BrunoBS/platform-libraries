package br.com.portalmanager.platform.library.audit.outbox;

import tools.jackson.databind.JsonNode;

/** Persists outbox entries using the entity and repository supplied by the consuming service. */
public abstract class AbstractAuditOutboxStore<E extends AuditOutboxEntity> implements AuditOutboxStore {

    private final AuditOutboxRepository<E> repository;

    protected AbstractAuditOutboxStore(AuditOutboxRepository<E> repository) {
        this.repository = repository;
    }

    @Override
    public final void append(JsonNode payload, JsonNode metadata) {
        repository.save(createEntry(payload, metadata));
    }

    protected abstract E createEntry(JsonNode payload, JsonNode metadata);
}
