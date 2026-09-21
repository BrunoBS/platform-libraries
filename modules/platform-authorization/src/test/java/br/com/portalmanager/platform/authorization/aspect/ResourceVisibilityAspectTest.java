package br.com.portalmanager.platform.authorization.aspect;

import br.com.portalmanager.platform.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.authorization.model.UserContext;
import br.com.portalmanager.platform.authorization.model.UserSession;
import br.com.portalmanager.platform.authorization.resource.AuthorizableResource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResourceVisibilityAspectTest {

    private final ResourceVisibilityAspect aspect = new ResourceVisibilityAspect();
    private final ResourceVisibility annotation = mock(ResourceVisibility.class);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldAllowOwnerWithoutFiltering() throws Throwable {
        UserSession session = session(Set.of("PM5_OWNER"), Set.of());
        UserContext.set(session);

        List<TestResource> resources = List.of(
                new TestResource("A-ONE"),
                new TestResource("A-TWO")
        );

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(resources);

        Object result = aspect.applyVisibility(joinPoint, annotation);

        assertSame(resources, result);
        verify(joinPoint).proceed();
    }

    @Test
    void shouldFilterCollectionByAuthorizerGroup() throws Throwable {
        UserSession session = session(
                Set.of("USER"),
                Set.of(new ParsedGroup("full", "profile", "env", "A-ONE"))
        );
        UserContext.set(session);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(List.of(
                new TestResource("A-ONE"),
                new TestResource("A-TWO")
        ));

        Object result = aspect.applyVisibility(joinPoint, annotation);

        assertInstanceOf(List.class, result);
        List<?> filtered = (List<?>) result;
        assertEquals(1, filtered.size());
        assertEquals("A-ONE", ((TestResource) filtered.getFirst()).getAuthorizerGroup());
    }

    @Test
    void shouldRejectUnauthorizedSingleResource() throws Throwable {
        UserSession session = session(
                Set.of("USER"),
                Set.of(new ParsedGroup("full", "profile", "env", "A-ONE"))
        );
        UserContext.set(session);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(new TestResource("A-TWO"));

        ForbiddenAccessException exception = assertThrows(
                ForbiddenAccessException.class,
                () -> aspect.applyVisibility(joinPoint, annotation)
        );

        assertEquals(AuthorizationMessageKeys.RESOURCE_ACCESS_DENIED, exception.getCode());
    }

    @Test
    void shouldRejectWhenSessionIsMissing() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> aspect.applyVisibility(joinPoint, annotation)
        );

        assertEquals(AuthorizationMessageKeys.SESSION_NOT_FOUND, exception.getCode());
        verifyNoInteractions(joinPoint);
    }

    private UserSession session(Set<String> groups, Set<ParsedGroup> authorizerGroups) {
        UserSession session = new UserSession();
        session.setGroups(groups);
        session.setAuthorizerGroups(authorizerGroups);
        return session;
    }

    private record TestResource(String authorizerGroup) implements AuthorizableResource {
        @Override
        public String getAuthorizerGroup() {
            return authorizerGroup;
        }
    }
}
