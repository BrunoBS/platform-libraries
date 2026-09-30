package br.com.portalmanager.platform.library.authorization.resource;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceVisibilityIdResolverTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final ResourceVisibilityIdResolver resolver = new ResourceVisibilityIdResolver(entityManager);
    private final ResourceVisibility visibility = mock(ResourceVisibility.class);

    @Test
    void shouldBuildDynamicQueryAndReturnOnlyVisibleIds() {
        when(visibility.table()).thenReturn("workspace_authorization");
        when(visibility.resourceIdColumn()).thenReturn("workspace_id");
        when(visibility.authorizerGroupColumn()).thenReturn("authorizer_group");

        String sql = "SELECT workspace_id FROM workspace_authorization "
                + "WHERE lower(authorizer_group) IN (?1, ?2)";

        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(sql)).thenReturn(query);
        when(query.setParameter(1, "team_a")).thenReturn(query);
        when(query.setParameter(2, "team_b")).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(10L, 20L, 20L));

        Set<Long> result = resolver.findAuthorizedIds(
                visibility,
                List.of("TEAM_A", "team_b", "TEAM_A")
        );

        assertEquals(Set.of(10L, 20L), result);
        verify(entityManager).createNativeQuery(sql);
        verify(query).setParameter(1, "team_a");
        verify(query).setParameter(2, "team_b");
    }

    @Test
    void shouldReturnEmptyWithoutQueryWhenThereAreNoAuthorizerGroups() {
        assertEquals(Set.of(), resolver.findAuthorizedIds(visibility, Set.of()));
        verify(entityManager, never()).createNativeQuery(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectUnsafeTableIdentifier() {
        when(visibility.table()).thenReturn("workspace_authorization;drop_table");
        when(visibility.resourceIdColumn()).thenReturn("workspace_id");
        when(visibility.authorizerGroupColumn()).thenReturn("authorizer_group");

        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.findAuthorizedIds(visibility, Set.of("TEAM_A"))
        );

        verify(entityManager, never()).createNativeQuery(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldAllowSchemaQualifiedTable() {
        when(visibility.table()).thenReturn("authorization.workspace_authorization");
        when(visibility.resourceIdColumn()).thenReturn("workspace_id");
        when(visibility.authorizerGroupColumn()).thenReturn("authorizer_group");

        String sql = "SELECT workspace_id FROM authorization.workspace_authorization "
                + "WHERE lower(authorizer_group) IN (?1)";

        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(sql)).thenReturn(query);
        when(query.setParameter(1, "team_a")).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(99));

        assertEquals(
                Set.of(99L),
                resolver.findAuthorizedIds(visibility, Set.of("TEAM_A"))
        );
    }
}
