package br.com.portalmanager.platform.library.authorization.client;

import br.com.portalmanager.platform.library.authorization.exception.AuthorizationServiceUnavailableException;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationClientTest {

    @Test
    void shouldReportTechnicalFailureWhenAuthorizationApiIsUnavailable() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        AuthorizationClient service = new AuthorizationClient(builder, "http://localhost:8080", new PlatformAuthorizationProperties.Retry());
        RuntimeException exception = new RuntimeException("Network timeout");
        AuthorizationRequest request = new AuthorizationRequest(
                "trace-1",
                "Bearer token",
                "workspace",
                "env",
                "app",
                AuthorizationAction.READ,
                AuthorizationLevel.ADM
        );

        AuthorizationServiceUnavailableException thrown = assertThrows(
                AuthorizationServiceUnavailableException.class,
                () -> service.recover(exception, request)
        );

        assertEquals("authorization.service.unavailable", thrown.getMessageKey());
        assertEquals(exception, thrown.getCause());
    }
}
