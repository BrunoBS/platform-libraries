package br.com.portalmanager.platform.library.authorization.client;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.exception.*;
import br.com.portalmanager.platform.library.authorization.model.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class AuthorizationClientHttpIntegrationTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test void sendsHeadersAndDeserializesAuthorizedSession() throws Exception {
        AtomicReference<String> headers = new AtomicReference<>();
        start(200, "{\"userName\":\"alice\"}", headers);
        UserSession session = client().authorize(request());
        assertThat(session).isNotNull();
        assertThat(session.getUserName()).isEqualTo("alice");
        assertThat(headers.get()).contains("Bearer token", "READ", "OPEN", "workspace-id", "request-id");
    }

    @Test void unauthorizedResponseFailsClosed() throws Exception {
        start(401, "{}", null);
        assertThatThrownBy(() -> client().authorize(request()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test void forbiddenResponseFailsClosed() throws Exception {
        start(403, "{}", null);
        assertThatThrownBy(() -> client().authorize(request()))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test void serverErrorFailsClosed() throws Exception {
        start(500, "{}", null);
        assertThatThrownBy(() -> client().authorize(request()))
                .isInstanceOf(AuthorizationServiceUnavailableException.class);
    }

    @Test void emptySuccessfulResponseDoesNotProduceSession() throws Exception {
        start(204, "", null);
        assertThat(client().authorize(request())).isNull();
    }

    private AuthorizationClient client() {
        var retry = new PlatformAuthorizationProperties.Retry();
        retry.setMaxRetries(0);
        return new AuthorizationClient(RestClient.builder(),
                "http://localhost:" + server.getAddress().getPort(), retry);
    }

    private static AuthorizationRequest request() {
        return new AuthorizationRequest("request-id", "Bearer token", "workspace-id",
                "environment-id", "application-id", AuthorizationAction.READ, AuthorizationLevel.OPEN);
    }

    private void start(int status, String body, AtomicReference<String> headers) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/authorize", exchange -> {
            if (headers != null) {
                headers.set(exchange.getRequestHeaders().toString());
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            if (status != 204) {
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            }
            exchange.close();
        });
        server.start();
    }
}
