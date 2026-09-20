package com.empresa.platform.testing.authorization;

import com.empresa.platform.authorization.model.UserSession;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Json;

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

    public void verifyCalledWithAccount(String accountId) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("X-Account-Id", equalTo(accountId)));
    }

    public void verifyCalledWithEnvironment(String environment) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("X-Environment", equalTo(environment)));
    }

    public void verifyCalledWithApplication(String applicationId) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("X-Application-Id", equalTo(applicationId)));
    }

    public void verifyCalledWithPolicy(String policy) {
        server.verify(postRequestedFor(urlEqualTo(AUTHORIZATION_PATH))
                .withHeader("X-Policy", equalTo(policy)));
    }
}
