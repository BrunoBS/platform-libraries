package br.com.portalmanager.platform.authorization.service;

import br.com.portalmanager.platform.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationClientServiceTest {

    @Test
    void shouldTriggerRecoverAndThrowForbiddenException() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        AuthorizationClientService service = new AuthorizationClientService(builder, "http://localhost:8080");

        RuntimeException exception = new RuntimeException("Network timeout");

        ForbiddenAccessException thrown = assertThrows(
                ForbiddenAccessException.class,
                () -> service.recover(
                        exception,
                        "trace-1",
                        "Bearer token",
                        "acc",
                        "env",
                        "app",
                        "GET",
                        AuthorizationLevel.ADM
                )
        );

        assertEquals(AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED, thrown.getCode());
        assertEquals(exception, thrown.getCause());
    }
}
