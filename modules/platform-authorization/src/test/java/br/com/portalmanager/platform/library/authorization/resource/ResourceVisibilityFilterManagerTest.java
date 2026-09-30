package br.com.portalmanager.platform.library.authorization.resource;

import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceVisibilityFilterManagerTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Session hibernateSession = mock(Session.class);
    private final Filter filter = mock(Filter.class);
    private final ResourceVisibilityFilterManager manager =
            new ResourceVisibilityFilterManager(entityManager);

    @Test
    void shouldNormalizeAuthorizersToUppercaseAndRemoveDuplicates() {
        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);
        when(hibernateSession.enableFilter(ResourceVisibilityFilterManager.FILTER_NAME))
                .thenReturn(filter);

        assertTrue(manager.enable(List.of("bbs-app", "BBS-APP", "catalog")));

        verify(filter).setParameterList(
                ResourceVisibilityFilterManager.PARAMETER_NAME,
                List.of("BBS-APP", "CATALOG")
        );
    }

    @Test
    void shouldAcceptExactlyFiveHundredDistinctAuthorizers() {
        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);
        when(hibernateSession.enableFilter(ResourceVisibilityFilterManager.FILTER_NAME))
                .thenReturn(filter);

        Set<String> groups = IntStream.range(0, ResourceVisibilityFilterManager.MAX_AUTHORIZER_GROUPS)
                .mapToObj(index -> "GROUP_" + index)
                .collect(Collectors.toSet());

        assertTrue(manager.enable(groups));
    }

    @Test
    void shouldRejectMoreThanFiveHundredDistinctAuthorizers() {
        Set<String> groups = IntStream.rangeClosed(0, ResourceVisibilityFilterManager.MAX_AUTHORIZER_GROUPS)
                .mapToObj(index -> "GROUP_" + index)
                .collect(Collectors.toSet());

        assertThrows(IllegalArgumentException.class, () -> manager.enable(groups));

        verify(entityManager, never()).unwrap(Session.class);
    }

    @Test
    void shouldUseNoAuthorizerSentinelForEmptyCollection() {
        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);
        when(hibernateSession.enableFilter(ResourceVisibilityFilterManager.FILTER_NAME))
                .thenReturn(filter);

        assertTrue(manager.enable(Set.of()));

        verify(filter).setParameterList(
                ResourceVisibilityFilterManager.PARAMETER_NAME,
                List.of(ResourceVisibilityFilterManager.NO_AUTHORIZER)
        );
    }

    @Test
    void shouldDisableEnabledFilter() {
        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);
        when(hibernateSession.getEnabledFilter(ResourceVisibilityFilterManager.FILTER_NAME))
                .thenReturn(filter);

        manager.disable();

        verify(hibernateSession).disableFilter(ResourceVisibilityFilterManager.FILTER_NAME);
    }
}
