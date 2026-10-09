package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationContextResolverTest {

    void endpoint(AuthorizationContext context) { }

    @Test
    void resolvesHeadersAndUriWithoutQueryString() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/catalog/status");
        request.setQueryString("active=true");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("Correlation-Id", "correlation");
        request.addHeader("workspace-identifier", "workspace");
        request.addHeader("environment-identifier", "environment");
        request.addHeader("application-identifier", "application");
        request.addHeader("User-Agent", "agent");
        request.setRemoteAddr("127.0.0.1");

        var resolver = new AuthorizationContextResolver(new PlatformAuthorizationProperties());
        var parameter = new MethodParameter(getClass().getDeclaredMethod("endpoint", AuthorizationContext.class), 0);
        assertThat(resolver.supportsParameter(parameter)).isTrue();

        var context = (AuthorizationContext) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(request), null);

        assertThat(context).isEqualTo(new AuthorizationContext(
                "correlation", "Bearer token", "workspace", "environment",
                "application", "127.0.0.1", "agent", "/api/catalog/status"));
    }

    @Test
    void missingHeadersRemainNull() throws Exception {
        var request = new MockHttpServletRequest("GET", "/catalog");
        var resolver = new AuthorizationContextResolver(new PlatformAuthorizationProperties());
        var parameter = new MethodParameter(getClass().getDeclaredMethod("endpoint", AuthorizationContext.class), 0);
        var context = (AuthorizationContext) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(request), null);
        assertThat(context.authorization()).isNull();
        assertThat(context.uri()).isEqualTo("/catalog");
    }
    @Test
    void honorsApplicationWideCustomHeaderNames() throws Exception {
        var properties = new PlatformAuthorizationProperties();
        properties.getHeaders().setWorkspaceIdentifier(List.of("X-Workspace-Id", "workspace-identifier"));
        var request = new MockHttpServletRequest("GET", "/catalog");
        request.addHeader("X-Workspace-Id", "custom-workspace");
        request.addHeader("workspace-identifier", "ignored-workspace");
        var resolver = new AuthorizationContextResolver(properties);
        var parameter = new MethodParameter(getClass().getDeclaredMethod("endpoint", AuthorizationContext.class), 0);

        var context = (AuthorizationContext) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(request), null);

        assertThat(context.workspaceIdentifier()).isEqualTo("custom-workspace");
    }

    @Test
    void usesAlternateHeaderOnlyWhenPrimaryIsMissing() throws Exception {
        var properties = new PlatformAuthorizationProperties();
        properties.getHeaders().setWorkspaceIdentifier("X-Workspace-Id");
        var request = new MockHttpServletRequest("GET", "/catalog");
        request.addHeader("workspace-identifier", "fallback");
        var resolver = new AuthorizationContextResolver(properties);
        var parameter = new MethodParameter(getClass().getDeclaredMethod("endpoint", AuthorizationContext.class), 0);

        var context = (AuthorizationContext) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(request), null);

        assertThat(context.workspaceIdentifier()).isEqualTo("fallback");

        request.addHeader("X-Workspace-Id", "primary");
        context = (AuthorizationContext) resolver.resolveArgument(
                parameter, null, new ServletWebRequest(request), null);
        assertThat(context.workspaceIdentifier()).isEqualTo("primary");
    }

}
