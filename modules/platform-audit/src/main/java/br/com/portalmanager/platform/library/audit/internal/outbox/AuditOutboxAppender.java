package br.com.portalmanager.platform.library.audit.internal.outbox;

import br.com.portalmanager.platform.library.audit.api.contract.outbox.AuditOutboxEntity;
import br.com.portalmanager.platform.library.audit.api.contract.outbox.AuditOutboxRepository;
import br.com.portalmanager.platform.library.audit.internal.message.AuditTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.api.exception.PlatformConfigurationException;
import jakarta.persistence.EntityManager;
import org.springframework.beans.BeanUtils;
import org.springframework.data.repository.support.Repositories;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.JsonNode;

import java.util.Objects;

/** Creates and stores an entry using the concrete outbox entity registered by the consumer. */
public final class AuditOutboxAppender {

    private final AuditOutboxRepository<?> repository;
    private final Class<? extends AuditOutboxEntity> entityType;
    private final ApplicationEventPublisher eventPublisher;

    public AuditOutboxAppender(
            AuditOutboxRepository<?> repository,
            EntityManager entityManager,
            Repositories repositories
    ) {
        this(repository, entityManager, repositories, null);
    }

    public AuditOutboxAppender(
            AuditOutboxRepository<?> repository,
            EntityManager entityManager,
            Repositories repositories,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = Objects.requireNonNull(repository, "audit outbox repository must not be null");
        this.eventPublisher = eventPublisher;
        Repositories repositoryRegistry = Objects.requireNonNull(repositories, "repositories must not be null");
        this.entityType = resolveEntityType(entityManager);
        validateRepository(repositoryRegistry);
    }

    public void append(JsonNode payload, JsonNode metadata) {
        AuditOutboxEntity entry = BeanUtils.instantiateClass(entityType);
        entry.prepareForPersistence(payload, metadata);
        repository.append(entry);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new AuditOutboxCreatedEvent(entry.getIdentifier()));
        }
    }

    private void validateRepository(Repositories repositories) {
        Object entityRepository = repositories.getRepositoryFor(entityType).orElse(null);
        if (entityRepository != repository) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.OUTBOX_REPOSITORY_MISMATCH);
        }
    }

    private static Class<? extends AuditOutboxEntity> resolveEntityType(EntityManager entityManager) {
        Class<? extends AuditOutboxEntity> resolvedEntityType = null;

        for (var entityType : entityManager.getMetamodel().getEntities()) {
            Class<?> candidateType = entityType.getJavaType();
            if (!AuditOutboxEntity.class.isAssignableFrom(candidateType)) {
                continue;
            }
            if (resolvedEntityType != null) {
                throw new PlatformConfigurationException(AuditTechnicalErrors.MULTIPLE_OUTBOX_ENTITIES);
            }
            resolvedEntityType = candidateType.asSubclass(AuditOutboxEntity.class);
        }

        if (resolvedEntityType == null) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.OUTBOX_ENTITY_REQUIRED);
        }

        return resolvedEntityType;
    }
}
