package br.com.portalmanager.platform.library.authorization.resource;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceVisibilityMappingRegistrarTest {

    @Test
    void shouldAttachFilterProgrammaticallyToAuthorizableEntity() {
        Metadata metadata = mock(Metadata.class);
        PersistentClass entity = mock(PersistentClass.class);

        Collection<PersistentClass> entityBindings = List.of(entity);
        doReturn(entityBindings).when(metadata).getEntityBindings();
        doReturn(TestResource.class).when(entity).getMappedClass();
        when(entity.getFilters()).thenReturn(List.of());

        int registered = ResourceVisibilityMappingRegistrar.register(metadata);

        assertEquals(1, registered);
        verify(entity).addFilter(
                ResourceVisibilityFilterManager.FILTER_NAME,
                ResourceVisibilityMappingRegistrar.DEFAULT_CONDITION,
                true,
                Map.of(),
                Map.of());
    }

    @Test
    void shouldIgnoreEntityThatDoesNotParticipateInVisibility() {
        Metadata metadata = mock(Metadata.class);
        PersistentClass entity = mock(PersistentClass.class);

        Collection<PersistentClass> entityBindings = List.of(entity);
        doReturn(entityBindings).when(metadata).getEntityBindings();
        doReturn(PlainEntity.class).when(entity).getMappedClass();

        assertEquals(0, ResourceVisibilityMappingRegistrar.register(metadata));
    }

    static final class TestResource implements AuthorizableResource {
        @Override
        public String getAuthorizerGroup() {
            return "test";
        }
    }

    static final class PlainEntity {
    }
}
