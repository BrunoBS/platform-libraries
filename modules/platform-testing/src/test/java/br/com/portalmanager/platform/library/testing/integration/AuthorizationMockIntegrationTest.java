package br.com.portalmanager.platform.library.testing.integration;

import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationSessionCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AuthorizationMockIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "platform.messaging.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration"
        }
)
@WithMockAuthorization
class AuthorizationMockIntegrationTest {

    @Autowired
    private AuthorizationMock authorizationMock;

    @Test
    void shouldReturnCustomizedPlatformSessionAndVerifyRequestContract() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authorizationMock.baseUrl() + "/authorize"))
                .header("X-Account-Id", "account-123")
                .header("X-Environment", "DEV")
                .header("X-Application-Id", "application-456")
                .header("X-Policy", "ADMIN")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("bruno.barbosa", "account-123", "PM5_OWNER");
        authorizationMock.verifyCalled();
        authorizationMock.verifyCalledWithAccount("account-123");
        authorizationMock.verifyCalledWithEnvironment("DEV");
        authorizationMock.verifyCalledWithApplication("application-456");
        authorizationMock.verifyCalledWithPolicy("ADMIN");
    }

    @Test
    void shouldMatchAuthorizationByResourceContextAndCustomHeader() throws Exception {
        authorizationMock.reset();
        authorizationMock.allowResource(resource -> resource
                        .workspace("workspace-123")
                        .application("application-456")
                        .environment("DEV")
                        .header("X-Custom-Resource", "custom-789"),
                session -> session.groups("RESOURCE_ALLOWED"));

        HttpRequest matching = HttpRequest.newBuilder()
                .uri(URI.create(authorizationMock.baseUrl() + "/authorize"))
                .header("X-Workspace-Id", "workspace-123")
                .header("X-Application-Id", "application-456")
                .header("X-Environment", "DEV")
                .header("X-Custom-Resource", "custom-789")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                matching, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("RESOURCE_ALLOWED");
    }

    @Test
    void shouldExposeUncalledVerification() {
        authorizationMock.verifyNotCalled();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        AuthorizationSessionCustomizer authorizationSessionCustomizer() {
            return session -> session
                    .userName("bruno.barbosa")
                    .accountId("account-123")
                    .groups("PM5_OWNER");
        }
    }
}
