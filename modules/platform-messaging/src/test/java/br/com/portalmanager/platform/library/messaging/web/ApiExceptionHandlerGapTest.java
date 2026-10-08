package br.com.portalmanager.platform.library.messaging.web;

import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ApiErrorResponse;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.model.ApiValidationDetail;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerGapTest {

    private static final Locale LOCALE = Locale.forLanguageTag("pt-BR");

    @Test
    void shouldUseRequestFormatMessageForInvalidNonEnumJsonValue() {
        ApiMessageResolver resolver = mock(ApiMessageResolver.class);
        HttpServletRequest request = request();
        InvalidFormatException invalidFormat = mock(InvalidFormatException.class);
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        when(invalidFormat.getTargetType()).thenReturn(Long.class);
        when(exception.getCause()).thenReturn(invalidFormat);
        when(resolver.resolve(PlatformMessageKeys.REQUEST_FORMAT_INVALID, LOCALE))
                .thenReturn(message(
                        "GLOBAL-0003",
                        PlatformMessageKeys.REQUEST_FORMAT_INVALID,
                        "O formato de um ou mais campos é incompatível com o esperado."
                ));

        ResponseEntity<ApiErrorResponse> response = new ApiExceptionHandler(resolver)
                .handleNotReadable(exception, LOCALE, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("GLOBAL-0003", response.getBody().code());
        assertEquals(
                "O formato de um ou mais campos é incompatível com o esperado.",
                response.getBody().message()
        );
    }

    @Test
    void shouldKeepUnreadableMessageForMalformedJsonWithoutInvalidValue() {
        ApiMessageResolver resolver = mock(ApiMessageResolver.class);
        HttpServletRequest request = request();
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        when(resolver.resolve(PlatformMessageKeys.REQUEST_NOT_READABLE, LOCALE))
                .thenReturn(message(
                        "GLOBAL-0002",
                        PlatformMessageKeys.REQUEST_NOT_READABLE,
                        "Não foi possível ler o corpo da requisição."
                ));

        ResponseEntity<ApiErrorResponse> response = new ApiExceptionHandler(resolver)
                .handleNotReadable(exception, LOCALE, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("GLOBAL-0002", response.getBody().code());
    }

    @Test
    void shouldIncludeResolvedCodeAndSolutionInStructuredValidationDetails() {
        ApiExceptionHandler handler = new ApiExceptionHandler(mock(ApiMessageResolver.class));
        ApiValidationDetail detail = new ApiValidationDetail(
                "version",
                "APPLICATION-0001",
                "A versão informada é inválida.",
                "Informe uma versão válida."
        );

        assertEquals(
                "[{\"field\":\"version\",\"code\":\"APPLICATION-0001\","
                        + "\"message\":\"A versão informada é inválida.\","
                        + "\"solution\":\"Informe uma versão válida.\"}]",
                handler.toJsonDetails(List.of(detail))
        );
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/applications");
        when(request.getHeader("Accept-Language")).thenReturn("pt-BR");
        when(request.getLocale()).thenReturn(LOCALE);
        return request;
    }

    private ApiMessage message(String code, String key, String text) {
        return new ApiMessage(code, key, "pt-BR", text, "Corrija os dados enviados.", 400);
    }
}
