package br.com.portalmanager.platform.library.audit.outbox;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.support.Repositories;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditOutboxAppenderTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Metamodel metamodel = mock(Metamodel.class);

    @Test
    void shouldCreateAndAppendTheSingleMappedOutboxEntity() throws Exception {
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        Set<EntityType<?>> entities = Set.of(entityType(TestAuditOutboxEntity.class));
        when(metamodel.getEntities()).thenReturn(entities);
        AuditOutboxRepository<TestAuditOutboxEntity> repository = mock(AuditOutboxRepository.class);
        Repositories repositories = mock(Repositories.class);
        when(repositories.getRepositoryFor(TestAuditOutboxEntity.class)).thenReturn(Optional.of(repository));
        AuditOutboxAppender appender = new AuditOutboxAppender(repository, entityManager, repositories);
        var mapper = new ObjectMapper();
        var payload = mapper.readTree("{\"identifier\":\"resource-1\"}");
        var metadata = mapper.readTree("{\"eventType\":\"CREATED\"}");

        appender.append(payload, metadata);

        var entry = org.mockito.ArgumentCaptor.forClass(TestAuditOutboxEntity.class);
        verify(repository).append(entry.capture());
        assertThat(entry.getValue().getIdentifier()).isNotBlank();
        assertThat(entry.getValue().getPayload()).isEqualTo(payload);
        assertThat(entry.getValue().getMetadata()).isEqualTo(metadata);
    }

    @Test
    void shouldFailWithConfigurationErrorWhenNoOutboxEntityIsMapped() {
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        when(metamodel.getEntities()).thenReturn(Set.of());

        assertThatThrownBy(() -> new AuditOutboxAppender(
                mockRepository(), entityManager, mock(Repositories.class)
        )).isInstanceOf(PlatformConfigurationException.class);
    }

    @Test
    void shouldFailWithConfigurationErrorWhenMultipleOutboxEntitiesAreMapped() {
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        Set<EntityType<?>> entities = Set.of(
                entityType(TestAuditOutboxEntity.class),
                entityType(OtherAuditOutboxEntity.class)
        );
        when(metamodel.getEntities()).thenReturn(entities);

        assertThatThrownBy(() -> new AuditOutboxAppender(
                mockRepository(), entityManager, mock(Repositories.class)
        )).isInstanceOf(PlatformConfigurationException.class);
    }

    @Test
    void shouldFailWithConfigurationErrorWhenRepositoryManagesAnotherEntity() {
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        Set<EntityType<?>> entities = Set.of(entityType(TestAuditOutboxEntity.class));
        when(metamodel.getEntities()).thenReturn(entities);
        AuditOutboxRepository<?> injectedRepository = mockRepository();
        AuditOutboxRepository<?> entityRepository = mockRepository();
        Repositories repositories = mock(Repositories.class);
        when(repositories.getRepositoryFor(TestAuditOutboxEntity.class))
                .thenReturn(Optional.of(entityRepository));

        assertThatThrownBy(() -> new AuditOutboxAppender(injectedRepository, entityManager, repositories))
                .isInstanceOf(PlatformConfigurationException.class);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static EntityType<?> entityType(Class<?> javaType) {
        EntityType entityType = mock(EntityType.class);
        when(entityType.getJavaType()).thenReturn(javaType);
        return entityType;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AuditOutboxRepository<?> mockRepository() {
        return mock(AuditOutboxRepository.class);
    }

    public static class TestAuditOutboxEntity extends AuditOutboxEntity {
        protected TestAuditOutboxEntity() {
        }
    }

    public static class OtherAuditOutboxEntity extends AuditOutboxEntity {
        protected OtherAuditOutboxEntity() {
        }
    }
}
