package br.com.portalmanager.platform.library.audit.outbox;

import jakarta.persistence.EntityManager;
import org.springframework.beans.BeanUtils;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

/** Creates and stores an entry using the concrete outbox entity registered by the consumer. */
public final class AuditOutboxAppender {

    private final AuditOutboxRepository<?> repository;
    private final Class<? extends AuditOutboxEntity> entityType;

    public AuditOutboxAppender(AuditOutboxRepository<?> repository, EntityManager entityManager) {
        this.repository = Objects.requireNonNull(repository, "audit outbox repository must not be null");
        this.entityType = resolveEntityType(entityManager);
    }

    public void append(JsonNode payload, JsonNode metadata) {
        AuditOutboxEntity entry = BeanUtils.instantiateClass(entityType);
        entry.prepareForPersistence(payload, metadata);
        repository.append(entry);
    }

    private static Class<? extends AuditOutboxEntity> resolveEntityType(EntityManager entityManager) {
        Class<? extends AuditOutboxEntity> resolvedEntityType = null;

        for (var entityType : entityManager.getMetamodel().getEntities()) {
            Class<?> candidateType = entityType.getJavaType();
            if (!AuditOutboxEntity.class.isAssignableFrom(candidateType)) {
                continue;
            }
            if (resolvedEntityType != null) {
                throw new IllegalStateException(
                        "Multiple JPA entities extend AuditOutboxEntity; exactly one is required");
            }
            resolvedEntityType = candidateType.asSubclass(AuditOutboxEntity.class);
        }

        if (resolvedEntityType == null) {
            throw new IllegalStateException(
                    "No JPA entity extends AuditOutboxEntity; register a concrete audit outbox entity");
        }

        return resolvedEntityType;
    }
}
