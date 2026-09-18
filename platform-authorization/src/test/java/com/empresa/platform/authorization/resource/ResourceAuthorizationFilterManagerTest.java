package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.model.ParsedGroup;
import com.empresa.platform.authorization.model.UserSession;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceAuthorizationFilterManagerTest {

    @Test
    void shouldEnableHibernateFilterWithNormalizedAuthorizers() {
        EntityManager entityManager = mock(EntityManager.class);
        Session hibernateSession = mock(Session.class);
        Filter filter = mock(Filter.class);

        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);
        when(hibernateSession.enableFilter(ResourceAuthorizationFilterManager.FILTER_NAME))
                .thenReturn(filter);

        UserSession session = new UserSession();
        session.setGroups(Set.of("USER"));
        session.setAuthorizerGroups(Set.of(
                new ParsedGroup("one", "DEV", "DEV", "GROUP-A"),
                new ParsedGroup("two", "DEV", "DEV", "group-b")
        ));

        ResourceAuthorizationFilterManager manager =
                new ResourceAuthorizationFilterManager(entityManager);

        assertTrue(manager.enable(session));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Collection> values = ArgumentCaptor.forClass(Collection.class);
        verify(filter).setParameterList(
                org.mockito.ArgumentMatchers.eq(ResourceAuthorizationFilterManager.PARAMETER_NAME),
                values.capture()
        );

        assertEquals(Set.of("group-a", "group-b"), Set.copyOf(values.getValue()));
    }

    @Test
    void shouldUseNonMatchingSentinelWhenSessionHasNoAuthorizers() {
        EntityManager entityManager = mock(EntityManager.class);
        Session hibernateSession = mock(Session.class);
        Filter filter = mock(Filter.class);

        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);
        when(hibernateSession.enableFilter(ResourceAuthorizationFilterManager.FILTER_NAME))
                .thenReturn(filter);

        UserSession session = new UserSession();
        session.setGroups(Set.of("USER"));

        ResourceAuthorizationFilterManager manager =
                new ResourceAuthorizationFilterManager(entityManager);

        assertTrue(manager.enable(session));

        verify(filter).setParameterList(
                ResourceAuthorizationFilterManager.PARAMETER_NAME,
                List.of(ResourceAuthorizationFilterManager.NO_AUTHORIZER)
        );
    }

    @Test
    void shouldNotEnableFilterForOwner() {
        EntityManager entityManager = mock(EntityManager.class);

        UserSession session = new UserSession();
        session.setGroups(Set.of("PM5_OWNER"));

        ResourceAuthorizationFilterManager manager =
                new ResourceAuthorizationFilterManager(entityManager);

        assertFalse(manager.enable(session));
        verifyNoInteractions(entityManager);
    }

    @Test
    void shouldDisableHibernateFilter() {
        EntityManager entityManager = mock(EntityManager.class);
        Session hibernateSession = mock(Session.class);
        when(entityManager.unwrap(Session.class)).thenReturn(hibernateSession);

        ResourceAuthorizationFilterManager manager =
                new ResourceAuthorizationFilterManager(entityManager);

        manager.disable();

        verify(hibernateSession).disableFilter(ResourceAuthorizationFilterManager.FILTER_NAME);
    }
}
