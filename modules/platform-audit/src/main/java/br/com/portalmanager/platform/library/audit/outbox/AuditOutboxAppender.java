package br.com.portalmanager.platform.library.audit.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.beans.BeanUtils;
import tools.jackson.databind.JsonNode;

import java.util.List;
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
        List<Class<?>> entityTypes = entityManager.getMetamodel().getEntities().stream()
                .map(EntityType::getJavaType)
                .filter(AuditOutboxEntity.class::isAssignableFrom)
                .toList();

        if (entityTypes.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one JPA entity extending AuditOutboxEntity, but found " + entityTypes.size());
        }

        return entityTypes.getFirst().asSubclass(AuditOutboxEntity.class);
    }
}
