package br.com.portalmanager.platform.library.authorization.resource;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceVisibilityMappingRegistrarTest {

    @Test
    void shouldResolvePhysicalColumnFromAnnotatedAttribute() {
        Metadata metadata = mock(Metadata.class);
        PersistentClass entity = mock(RootClass.class);
        Property property = mock(Property.class);
        Column column = new Column("meu_authorizer");

        Collection<PersistentClass> entityBindings = List.of(entity);
        doReturn(entityBindings).when(metadata).getEntityBindings();
        doReturn(TestResource.class).when(entity).getMappedClass();
        when(entity.getFilters()).thenReturn(List.of());
        when(entity.getProperty("qualquerNome")).thenReturn(property);
        when(property.getColumnSpan()).thenReturn(1);
        when(property.getSelectables()).thenReturn(List.of(column));

        int registered = ResourceVisibilityMappingRegistrar.register(metadata);

        assertEquals(1, registered);
        verify(entity).addFilter(
                ResourceVisibilityFilterManager.filterName(TestResource.class),
                "meu_authorizer in (:authorizerGroups)",
                true,
                Map.of(),
                Map.of());
    }

    @Test
    void shouldIgnoreEntityWithoutAuthorizerGroupAttribute() {
        Metadata metadata = mock(Metadata.class);
        PersistentClass entity = mock(RootClass.class);

        Collection<PersistentClass> entityBindings = List.of(entity);
        doReturn(entityBindings).when(metadata).getEntityBindings();
        doReturn(PlainEntity.class).when(entity).getMappedClass();

        assertEquals(0, ResourceVisibilityMappingRegistrar.register(metadata));
    }

    @Test
    void shouldRejectEntityWithMoreThanOneAuthorizerGroupAttribute() {
        Metadata metadata = mock(Metadata.class);
        PersistentClass entity = mock(RootClass.class);

        Collection<PersistentClass> entityBindings = List.of(entity);
        doReturn(entityBindings).when(metadata).getEntityBindings();
        doReturn(InvalidResource.class).when(entity).getMappedClass();

        assertThrows(
                IllegalStateException.class,
                () -> ResourceVisibilityMappingRegistrar.register(metadata)
        );
    }

    @Test
    void shouldRejectNonStringAuthorizerGroupAttribute() {
        Metadata metadata = mock(Metadata.class);
        PersistentClass entity = mock(RootClass.class);

        Collection<PersistentClass> entityBindings = List.of(entity);
        doReturn(entityBindings).when(metadata).getEntityBindings();
        doReturn(InvalidTypeResource.class).when(entity).getMappedClass();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ResourceVisibilityMappingRegistrar.register(metadata)
        );

        assertEquals(
                "@AuthorizerGroup field "
                        + InvalidTypeResource.class.getName()
                        + ".authorizer must be java.lang.String",
                exception.getMessage()
        );
    }

    static final class TestResource {
        @AuthorizerGroup
        private String qualquerNome;
    }

    static final class PlainEntity {
    }

    static final class InvalidResource {
        @AuthorizerGroup
        private String first;

        @AuthorizerGroup
        private String second;
    }

    static final class InvalidTypeResource {
        @AuthorizerGroup
        private Long authorizer;
    }
}
