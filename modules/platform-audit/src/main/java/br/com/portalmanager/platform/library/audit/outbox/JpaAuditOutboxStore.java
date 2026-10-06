package br.com.portalmanager.platform.library.audit.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.beans.BeanUtils;
import tools.jackson.databind.JsonNode;

import java.util.List;

/** Persists outbox entries using the concrete entity mapped by the consuming service. */
public final class JpaAuditOutboxStore implements AuditOutboxStore {

    private final EntityManager entityManager;
    private final Class<? extends AuditOutboxEntity> entityType;

    public JpaAuditOutboxStore(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.entityType = resolveEntityType(entityManager);
    }

    @Override
    public void append(JsonNode payload, JsonNode metadata) {
        AuditOutboxEntity entry = BeanUtils.instantiateClass(entityType);
        entry.prepareForPersistence(payload, metadata);
        entityManager.persist(entry);
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
