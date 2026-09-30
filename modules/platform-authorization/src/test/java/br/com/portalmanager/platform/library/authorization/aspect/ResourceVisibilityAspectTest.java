package br.com.portalmanager.platform.library.authorization.aspect;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.resource.ResourceVisibilityFilterManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceVisibilityAspectTest {

    private final ResourceVisibilityFilterManager filterManager = mock(ResourceVisibilityFilterManager.class);
    private final ResourceVisibilityAspect aspect = new ResourceVisibilityAspect(filterManager);
    private final ResourceVisibility annotation = mock(ResourceVisibility.class);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldAllowOwnerWithoutEnablingFilter() throws Throwable {
        UserSession session = session(
                Set.of("PM5_OWNER"),
                Set.of(new ParsedGroup("PM5-ENG-DEV_BBS-APP", "ENG", "DEV", "BBS-APP"))
        );
        UserContext.set(session);

        Object expected = new Object();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(expected);

        Object result = aspect.applyVisibility(joinPoint, annotation);

        assertSame(expected, result);
        verify(joinPoint).proceed();
        verifyNoInteractions(filterManager);
    }

    @Test
    void shouldEnableAndDisableHibernateFilterWithParsedAuthorizers() throws Throwable {
        UserSession session = session(
                Set.of("USER"),
                Set.of(
                        new ParsedGroup("PM5-ENG-DEV_BBS-APP", "ENG", "DEV", "BBS-APP"),
                        new ParsedGroup("PM5-NEG-DEV_CATALOG", "NEG", "DEV", "CATALOG")
                )
        );
        UserContext.set(session);

        Object expected = new Object();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(expected);
        when(filterManager.enable(Set.of("BBS-APP", "CATALOG"))).thenReturn(true);

        Object result = aspect.applyVisibility(joinPoint, annotation);

        assertSame(expected, result);
        verify(filterManager).enable(Set.of("BBS-APP", "CATALOG"));
        verify(joinPoint).proceed();
        verify(filterManager).disable();
    }

    @Test
    void shouldDisableHibernateFilterWhenProceedThrows() throws Throwable {
        UserSession session = session(
                Set.of("USER"),
                Set.of(new ParsedGroup("PM5-ENG-DEV_BBS-APP", "ENG", "DEV", "BBS-APP"))
        );
        UserContext.set(session);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        RuntimeException expected = new RuntimeException("boom");
        when(joinPoint.proceed()).thenThrow(expected);
        when(filterManager.enable(Set.of("BBS-APP"))).thenReturn(true);

        RuntimeException result = assertThrows(
                RuntimeException.class,
                () -> aspect.applyVisibility(joinPoint, annotation)
        );

        assertSame(expected, result);
        verify(filterManager).enable(Set.of("BBS-APP"));
        verify(filterManager).disable();
    }

    @Test
    void shouldNotDisableFilterWhenManagerDidNotEnableIt() throws Throwable {
        UserSession session = session(
                Set.of("USER"),
                Set.of(new ParsedGroup("PM5-ENG-DEV_BBS-APP", "ENG", "DEV", "BBS-APP"))
        );
        UserContext.set(session);

        Object expected = new Object();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(expected);
        when(filterManager.enable(Set.of("BBS-APP"))).thenReturn(false);

        Object result = aspect.applyVisibility(joinPoint, annotation);

        assertSame(expected, result);
        verify(filterManager).enable(Set.of("BBS-APP"));
        verify(filterManager, never()).disable();
    }

    @Test
    void shouldRejectWhenSessionIsMissing() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> aspect.applyVisibility(joinPoint, annotation)
        );

        assertEquals(AuthorizationMessageKeys.SESSION_NOT_FOUND, exception.getCode());
        verifyNoInteractions(joinPoint, filterManager);
    }

    @Test
    void shouldRejectWhenAuthorizerGroupsAreMissing() {
        UserSession session = session(Set.of("USER"), Set.of());
        UserContext.set(session);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> aspect.applyVisibility(joinPoint, annotation)
        );

        assertEquals(AuthorizationMessageKeys.GROUPS_NOT_FOUND, exception.getCode());
        verifyNoInteractions(joinPoint, filterManager);
    }

    private UserSession session(Set<String> groups, Set<ParsedGroup> authorizerGroups) {
        UserSession session = new UserSession();
        session.setGroups(groups);
        session.setAuthorizerGroups(authorizerGroups);
        return session;
    }
}
