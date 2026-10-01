package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationPolicy;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequest;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.registry.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.authorization.service.AuthorizationClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationInterceptorTest {

    @Mock
    private AuthorizationClientService clientService;

    @Mock
    private AuthorizationMetadataRegistry registry;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HandlerMethod handlerMethod;

    private AuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthorizationInterceptor(
                clientService,
                registry
        );

        MDC.clear();
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        UserContext.clear();
    }

    @Test
    void shouldThrowExceptionWhenCorrelationIdIsMissing()
            throws Exception {

        configureHandlerMethod();

        when(request.getHeader("correlationId"))
                .thenReturn(null);

        assertThrows(
                UnauthorizedAccessException.class,
                () -> interceptor.preHandle(
                        request,
                        response,
                        handlerMethod
                )
        );

        verifyNoInteractions(clientService);
    }

    @Test
    void shouldThrowExceptionWhenAuthorizationIsMissing()
            throws Exception {

        configureHandlerMethod();

        when(request.getHeader("correlationId"))
                .thenReturn("12345");

        when(request.getHeader("Authorization"))
                .thenReturn(null);

        assertThrows(
                UnauthorizedAccessException.class,
                () -> interceptor.preHandle(
                        request,
                        response,
                        handlerMethod
                )
        );

        verifyNoInteractions(clientService);
    }

    @Test
    void shouldThrowExceptionWhenAuthorizationIsNotBearer()
            throws Exception {

        configureHandlerMethod();

        when(request.getHeader("correlationId"))
                .thenReturn("12345");

        when(request.getHeader("Authorization"))
                .thenReturn("Basic abc123");

        assertThrows(
                UnauthorizedAccessException.class,
                () -> interceptor.preHandle(
                        request,
                        response,
                        handlerMethod
                )
        );

        verifyNoInteractions(clientService);
    }

    @Test
    void shouldAuthorizeAndPopulateContextWhenHeadersAreValid()
            throws Exception {

        configureHandlerMethod();

        when(request.getHeader("correlationId"))
                .thenReturn("12345");

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");

        when(request.getHeader("User-Agent"))
                .thenReturn("JUnit");

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/v1/resource");

        when(request.getRemoteAddr())
                .thenReturn("127.0.0.1");

        Map<String, String> pathVariables = new HashMap<>();

        pathVariables.put(
                "workspaceIdentifier",
                "account-123"
        );

        pathVariables.put(
                "environmentIdentifier",
                "env-123"
        );

        pathVariables.put(
                "applicationIdentifier",
                "app-123"
        );

        when(request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        )).thenReturn(pathVariables);

        AuthorizationPolicy policy =
                new AuthorizationPolicy(
                        AuthorizationLevel.DEV,
                        AuthorizationPolicy.Source.DEFAULT
                );

        when(registry.resolve(any(), any())).thenReturn(policy);

        UserSession session = new UserSession();

        session.setTraceId("12345");
        session.setUserName("bruno");
        session.setAccountId("account-123");
        session.setApplicationId("app-123");
        session.setEnvironmentId("env-123");

        when(clientService.authorize(new AuthorizationRequest(
                "12345", "Bearer valid-token", "account-123", "env-123", "app-123", "GET", AuthorizationLevel.DEV
        ))).thenReturn(session);

        boolean result =
                interceptor.preHandle(
                        request,
                        response,
                        handlerMethod
                );

        assertTrue(result);

        UserSession context =
                UserContext.get().orElseThrow();

        assertEquals(
                "12345",
                context.getTraceId()
        );

        assertEquals(
                "bruno",
                context.getUserName()
        );

        assertEquals(
                "account-123",
                context.getAccountId()
        );

        assertEquals(
                "env-123",
                context.getEnvironmentId()
        );

        assertEquals(
                "app-123",
                context.getApplicationId()
        );

        assertEquals(
                "12345",
                MDC.get("correlationId")
        );

        assertEquals(
                "bruno",
                MDC.get("username")
        );

        assertEquals(
                "127.0.0.1",
                MDC.get("clientIp")
        );

        assertEquals(
                "JUnit",
                MDC.get("userAgent")
        );

        assertEquals(
                "/api/v1/resource",
                MDC.get("uri")
        );

        assertEquals(
                "account-123",
                MDC.get("accountId")
        );

        assertEquals(
                "env-123",
                MDC.get("environmentId")
        );

        assertEquals(
                "app-123",
                MDC.get("applicationId")
        );

        verify(registry).resolve(String.class, methodForHandler());

        verify(clientService).authorize(new AuthorizationRequest(
                "12345", "Bearer valid-token", "account-123", "env-123", "app-123", "GET", AuthorizationLevel.DEV
        ));
    }

    @Test
    void shouldForwardOpenPolicyToAuthorizationApiWithoutLocalDecision() throws Exception {
        configureHandlerMethod();

        when(request.getHeader("correlationId")).thenReturn("open-correlation");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/open-resource");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(null);

        AuthorizationPolicy policy = new AuthorizationPolicy(
                AuthorizationLevel.OPEN,
                AuthorizationPolicy.Source.DEFAULT
        );
        when(registry.resolve(String.class, methodForHandler())).thenReturn(policy);

        UserSession session = new UserSession();
        session.setTraceId("open-correlation");
        session.setUserName("bruno");

        when(clientService.authorize(new AuthorizationRequest(
                "open-correlation", "Bearer valid-token", null, null, null, "GET", AuthorizationLevel.OPEN
        ))).thenReturn(session);

        assertTrue(interceptor.preHandle(request, response, handlerMethod));

        verify(clientService).authorize(new AuthorizationRequest(
                "open-correlation", "Bearer valid-token", null, null, null, "GET", AuthorizationLevel.OPEN
        ));
    }

    @Test
    void shouldAuthorizeWithoutPathVariables()
            throws Exception {

        configureHandlerMethod();

        when(request.getHeader("correlationId"))
                .thenReturn("12345");

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");

        when(request.getHeader("User-Agent"))
                .thenReturn("JUnit");

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/v1/resource");

        when(request.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        )).thenReturn(null);

        AuthorizationPolicy policy =
                new AuthorizationPolicy(
                        AuthorizationLevel.DEV,
                        AuthorizationPolicy.Source.DEFAULT
                );

        when(registry.resolve(String.class, methodForHandler())).thenReturn(policy);

        UserSession session = new UserSession();

        session.setTraceId("12345");
        session.setUserName("bruno");

        when(clientService.authorize(new AuthorizationRequest(
                "12345", "Bearer valid-token", null, null, null, "GET", AuthorizationLevel.DEV
        ))).thenReturn(session);

        boolean result =
                interceptor.preHandle(
                        request,
                        response,
                        handlerMethod
                );

        assertTrue(result);

        assertEquals(
                "12345",
                MDC.get("correlationId")
        );

        verify(clientService).authorize(new AuthorizationRequest(
                "12345", "Bearer valid-token", null, null, null, "GET", AuthorizationLevel.DEV
        ));
    }

    @Test
    void shouldReturnTrueWhenHandlerIsNotHandlerMethod()
            throws Exception {

        Object handler = new Object();

        boolean result =
                interceptor.preHandle(
                        request,
                        response,
                        handler
                );

        assertTrue(result);

        verifyNoInteractions(
                clientService,
                registry
        );

        assertTrue(
                UserContext.get().isEmpty()
        );

        assertNull(
                MDC.get("username")
        );
    }

    @Test
    void shouldUseUnknownWhenUserAgentIsMissing()
            throws Exception {

        configureHandlerMethod();

        when(request.getHeader("correlationId"))
                .thenReturn("12345");

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");

        when(request.getHeader("User-Agent"))
                .thenReturn(null);

        when(request.getMethod())
                .thenReturn("GET");

        when(request.getRequestURI())
                .thenReturn("/api/v1/resource");

        when(request.getRemoteAddr())
                .thenReturn("127.0.0.1");

        when(request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        )).thenReturn(new HashMap<>());

        AuthorizationPolicy policy =
                new AuthorizationPolicy(
                        AuthorizationLevel.DEV,
                        AuthorizationPolicy.Source.DEFAULT
                );

        when(registry.resolve(String.class, methodForHandler())).thenReturn(policy);

        UserSession session = new UserSession();

        session.setTraceId("12345");
        session.setUserName("bruno");

        when(clientService.authorize(new AuthorizationRequest(
                "12345", "Bearer valid-token", null, null, null, "GET", AuthorizationLevel.DEV
        ))).thenReturn(session);

        boolean result =
                interceptor.preHandle(
                        request,
                        response,
                        handlerMethod
                );

        assertTrue(result);

        assertEquals(
                "unknown",
                MDC.get("userAgent")
        );
    }

    @Test
    void shouldClearContextAndMdcAfterCompletion() {

        UserSession session = new UserSession();

        session.setUserName("bruno");

        UserContext.set(session);

        MDC.put(
                "username",
                "bruno"
        );

        assertTrue(
                UserContext.get().isPresent()
        );

        assertEquals(
                "bruno",
                MDC.get("username")
        );

        interceptor.afterCompletion(
                request,
                response,
                handlerMethod,
                null
        );

        assertTrue(
                UserContext.get().isEmpty(),
                "O contexto deveria ter sido limpo no afterCompletion."
        );

        assertNull(
                MDC.get("username"),
                "O MDC deveria ter sido limpo."
        );
    }

    private void configureHandlerMethod()
            throws NoSuchMethodException {

        doReturn(String.class)
                .when(handlerMethod)
                .getBeanType();

        doReturn(methodForHandler())
                .when(handlerMethod)
                .getMethod();
    }

    private Method methodForHandler()
            throws NoSuchMethodException {

        return String.class.getMethod("toString");
    }
}
