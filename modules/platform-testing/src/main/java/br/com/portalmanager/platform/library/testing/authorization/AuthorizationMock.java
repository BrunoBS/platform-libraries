package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.model.UserSession;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Json;

import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public final class AuthorizationMock {

    private static final String AUTHORIZATION_PATH = "/authorize";
    private static final String CONTENT_TYPE = "application/json";

    private final WireMockServer server;

    public AuthorizationMock(WireMockServer server) {
        this.server = server;
    }

    public void reset() {
        server.resetAll();
    }

    public void allow() {
        allow(builder -> {
        });
    }

    public void allow(AuthorizationSessionCustomizer customizer) {
        AuthorizationSessionBuilder builder = AuthorizationSessionBuilder.builder();
        customizer.customize(builder);
        allow(builder.build());
    }

    public void allow(UserSession session) {
        custom(200, Json.write(session));
    }

    public void allowResource(Consumer<AuthorizationResourceMatcher> resource,
                              AuthorizationSessionCustomizer customizer) {
        AuthorizationSessionBuilder builder = AuthorizationSessionBuilder.builder();
        customizer.customize(builder);
        customResource(resource, 200, Json.write(builder.build()));
    }

    public void forbiddenResource(Consumer<AuthorizationResourceMatcher> resource) {
        customResource(resource, 403, "{\"message\":\"Permissão insuficiente\"}");
    }

    public void denyResource(Consumer<AuthorizationResourceMatcher> resource) {
        customResource(resource, 401, "{\"message\":\"Acesso não permitido\"}");
    }

    public void customResource(Consumer<AuthorizationResourceMatcher> resource, int status, String body) {
        AuthorizationResourceMatcher matcher = new AuthorizationResourceMatcher();
        resource.accept(matcher);
        var mapping = post(urlEqualTo(AUTHORIZATION_PATH));
        for (var header : matcher.headers().entrySet()) {
            mapping = mapping.withHeader(header.getKey(), equalTo(header.getValue()));
        }
        server.stubFor(mapping.willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", CONTENT_TYPE)
                .withBody(body)));
    }

    public void deny() {
        custom(401, "{\"message\":\"Acesso não permitido\"}");
    }

    public void forbidden() {
        custom(403, "{\"message\":\"Permissão insuficiente\"}");
    }

    public void internalError() {
        custom(500, "{\"message\":\"Erro no serviço de autorização\"}");
    }

    public void expiredSession() {
        allow(AuthorizationSessionBuilder.builder().expired().build());
    }

    public void custom(int status, String body) {
        server.stubFor(post(urlEqualTo(AUTHORIZATION_PATH))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", CONTENT_TYPE)
                        .withBody(body)));
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    public void verifyCalled() {
        verifyCalled(1);
    }

    public void verifyCalled(int times) {
        server.verify(times, postRequestedFor(urlEqualTo(AUTHORIZATION_PATH)));
    }

    public void verifyNotCalled() {
        verifyCalled(0);
    }

    public void verifyCalledWithWorkspace(String workspaceIdentifier) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("workspaceIdentifier", equalTo(workspaceIdentifier)));
    }

    @Deprecated(forRemoval = false)
    public void verifyCalledWithAccount(String accountId) {
        verifyCalledWithWorkspace(accountId);
    }

    public void verifyCalledWithEnvironment(String environment) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("environmentIdentifier", equalTo(environment)));
    }

    public void verifyCalledWithApplication(String applicationId) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("applicationIdentifier", equalTo(applicationId)));
    }

    public void verifyCalledWithPolicy(String policy) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("policy", equalTo(policy)));
    }
}
