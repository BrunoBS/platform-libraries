package br.com.portalmanager.platform.library.messaging.web;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.model.ApiErrorResponse;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerValidationDetailTest {

    @Test
    void shouldIncludeResolvedCodeAndSolutionForEachValidationDetail() {
        ApiMessageResolver resolver = mock(ApiMessageResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Locale locale = Locale.forLanguageTag("pt-BR");

        when(request.getRequestURI()).thenReturn("/api/v1/applications/123");
        when(request.getHeader("Accept-Language")).thenReturn("pt-BR");
        when(request.getLocale()).thenReturn(locale);
        when(resolver.resolve("global.validation.failed", locale))
                .thenReturn(new ApiMessage(
                        "GLOBAL-0001",
                        "global.validation.failed",
                        "pt-BR",
                        "Um ou mais campos são inválidos.",
                        "Corrija os campos.",
                        400
                ));
        when(resolver.resolve("application.version.required", locale))
                .thenReturn(new ApiMessage(
                        "APPLICATION-0001",
                        "application.version.required",
                        "pt-BR",
                        "A versão {0} é obrigatória.",
                        "Informe a versão {0} recebida na última consulta.",
                        400
                ));

        ValidationException exception = new ValidationException(
                "global.validation.failed",
                List.of(new ValidationDetail(
                        "version",
                        "application.version.required",
                        Map.of("0", "atual")
                ))
        );

        ApiExceptionHandler handler = new ApiExceptionHandler(resolver);
        ResponseEntity<ApiErrorResponse> response = handler.handle(exception, locale, request);

        assertEquals(400, response.getStatusCode().value());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("GLOBAL-0001", body.code());
        assertEquals(1, body.details().size());
        assertEquals("version", body.details().getFirst().field());
        assertEquals("APPLICATION-0001", body.details().getFirst().code());
        assertEquals("A versão atual é obrigatória.", body.details().getFirst().message());
        assertEquals(
                "Informe a versão atual recebida na última consulta.",
                body.details().getFirst().solution()
        );
    }
}
