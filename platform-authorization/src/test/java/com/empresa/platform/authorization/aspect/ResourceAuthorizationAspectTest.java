package com.empresa.platform.authorization.aspect;

import com.empresa.platform.authorization.annotation.ResourceAuthorization;
import com.empresa.platform.authorization.model.ParsedGroup;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;
import com.empresa.platform.authorization.resource.AuthorizableResource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.empresa.platform.messaging.exception.ForbiddenException;
import com.empresa.platform.messaging.exception.UnauthorizedException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceAuthorizationAspectTest {

    private final ResourceAuthorizationAspect aspect = new ResourceAuthorizationAspect();
    private final ResourceAuthorization annotation = mock(ResourceAuthorization.class);

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

        Object result = aspect.authorize(joinPoint, annotation);

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

        Object result = aspect.authorize(joinPoint, annotation);

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

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> aspect.authorize(joinPoint, annotation)
        );

        assertEquals("authorization.resource.access.denied", exception.getMessageKey());
    }

    @Test
    void shouldRejectWhenSessionIsMissing() {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> aspect.authorize(joinPoint, annotation)
        );

        assertEquals("authorization.session.not.found", exception.getMessageKey());
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
