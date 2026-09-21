package br.com.portalmanager.platform.messaging.web;

import br.com.portalmanager.platform.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.messaging.model.ApiErrorResponse;
import br.com.portalmanager.platform.messaging.model.ApiMessage;
import br.com.portalmanager.platform.messaging.resolver.ApiMessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OptimisticLockExceptionHandlerTest {

    @Test
    void shouldReturnConflictForOptimisticLockingFailure() {
        ApiMessageResolver resolver = mock(ApiMessageResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        ApiExceptionHandler handler = new ApiExceptionHandler(resolver);
        Locale locale = Locale.of("pt", "BR");

        when(request.getRequestURI()).thenReturn("/api/v1/accounts/10");
        when(request.getHeader("Accept-Language")).thenReturn("pt-BR");
        when(request.getLocale()).thenReturn(locale);
        when(resolver.resolve(PlatformMessageKeys.RESOURCE_VERSION_CONFLICT, locale))
                .thenReturn(new ApiMessage(
                        "GLOBAL-0009",
                        PlatformMessageKeys.RESOURCE_VERSION_CONFLICT,
                        "pt-BR",
                        "O recurso foi alterado desde a última consulta.",
                        "Atualize os dados e tente novamente.",
                        409
                ));

        ResponseEntity<ApiErrorResponse> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("stale version"),
                locale,
                request
        );

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("GLOBAL-0009", response.getBody().code());
    }
}
