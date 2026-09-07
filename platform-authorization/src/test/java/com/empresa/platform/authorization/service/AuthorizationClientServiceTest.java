package com.empresa.platform.authorization.service;

import com.empresa.platform.authorization.model.AuthorizationLevel;
import com.empresa.platform.messaging.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationClientServiceTest {

    @Test
    void shouldTriggerRecoverAndThrowForbiddenException() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        AuthorizationClientService service = new AuthorizationClientService(builder, "http://localhost:8080");

        RuntimeException exception = new RuntimeException("Network timeout");
        
        ForbiddenException thrown = assertThrows(ForbiddenException.class, () -> 
            service.recover(exception, "trace-1", "Bearer token", "acc", "env", "app", "GET", AuthorizationLevel.ADM)
        );

        assertEquals("PLATFORM_ACCESS_DENIED", thrown.getMessageKey());
        assertEquals(exception, thrown.getCause());
    }
}
