package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.model.ParsedGroup;
import com.empresa.platform.authorization.model.UserSession;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ResourceVisibilityFilterManagerTest {

    @Test
    void ownerShouldBypassWithoutTouchingHibernateSession() {
        EntityManager entityManager = mock(EntityManager.class);
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityFilterManager manager =
                new ResourceVisibilityFilterManager(entityManager, context);
        UserSession owner = new UserSession();
        owner.setGroups(Set.of("PM5_OWNER"));

        assertThat(manager.enable(owner)).isFalse();
        assertThat(context.isActive()).isFalse();
        verifyNoInteractions(entityManager);
    }

    @Test
    void nestedDisableShouldKeepOuterNativeContextActive() {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> null).when(session).doWork(any());
        when(session.enableFilter(ResourceVisibilityFilterManager.FILTER_NAME)).thenReturn(filter);
        when(session.getEnabledFilter(ResourceVisibilityFilterManager.FILTER_NAME)).thenReturn(filter);

        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityFilterManager manager =
                new ResourceVisibilityFilterManager(entityManager, context);

        UserSession sessionData = userSession("INVESTIMENTOS");
        assertThat(manager.enable(sessionData)).isTrue();
        assertThat(manager.enable(sessionData)).isTrue();

        manager.disable();

        assertThat(context.isActive()).isTrue();

        manager.disable();

        assertThat(context.isActive()).isFalse();
        verify(session, times(1)).disableFilter(ResourceVisibilityFilterManager.FILTER_NAME);
    }

    @Test
    void setupFailureShouldNotActivateNativeContext() {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> null).when(session).doWork(any());
        when(session.enableFilter(ResourceVisibilityFilterManager.FILTER_NAME))
                .thenThrow(new IllegalStateException("filter setup failed"));

        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityFilterManager manager =
                new ResourceVisibilityFilterManager(entityManager, context);

        assertThatThrownBy(() -> manager.enable(userSession("INVESTIMENTOS")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("filter setup failed");

        assertThat(context.isActive()).isFalse();
    }

    @Test
    void cleanupFailureShouldBeSuppressedAndPreserveOriginalSetupFailure() {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        doAnswer(invocation -> null)
                .doThrow(new IllegalStateException("cleanup failed"))
                .when(session).doWork(any());
        when(session.enableFilter(ResourceVisibilityFilterManager.FILTER_NAME))
                .thenThrow(new IllegalStateException("filter setup failed"));

        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityFilterManager manager =
                new ResourceVisibilityFilterManager(entityManager, context);

        assertThatThrownBy(() -> manager.enable(userSession("INVESTIMENTOS")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("filter setup failed")
                .satisfies(exception -> assertThat(exception.getSuppressed())
                        .singleElement()
                        .hasMessage("cleanup failed"));

        assertThat(context.isActive()).isFalse();
    }

    private UserSession userSession(String authorizer) {
        UserSession session = new UserSession();
        session.setGroups(Set.of("RESOURCE_VISIBILITY"));
        session.setAuthorizerGroups(Set.of(
                new ParsedGroup("POC-" + authorizer, "POC", "DEV", authorizer)
        ));
        return session;
    }
}
