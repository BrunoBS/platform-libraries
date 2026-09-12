package com.empresa.platform.testing.authorization;

import com.empresa.platform.authorization.model.UserSession;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Json;

import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
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

    public void allow(Consumer<AuthorizationSessionBuilder> customizer) {
        AuthorizationSessionBuilder builder = AuthorizationSessionBuilder.builder();
        customizer.accept(builder);
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
}
