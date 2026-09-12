package com.empresa.platform.messaging.web;

import com.empresa.platform.messaging.exception.ApiException;
import com.empresa.platform.messaging.exception.ApiMessageNotFoundException;
import com.empresa.platform.messaging.exception.NotFoundException;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.model.ApiErrorResponse;
import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.model.ValidationDetail;
import com.empresa.platform.messaging.resolver.ApiMessageResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiExceptionHandlerTest {

    private ApiMessageResolver resolver;
    private HttpServletRequest request;
    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        resolver = mock(ApiMessageResolver.class);
        request = mock(HttpServletRequest.class);
        handler = new ApiExceptionHandler(resolver);
        MDC.clear(); // Garante isolamento limpando o MDC antes de cada teste
    }

    @Test
    void shouldHandleApiExceptionWithoutCauseAndWithMdcKey() {
        // Arrange
        String traceKey = "traceId";
        MDC.put(traceKey, "MDC-12345");

        when(request.getRequestURI()).thenReturn("/api/v1/users");
        when(resolver.resolve("USER_NOT_FOUND", Locale.of("pt", "BR")))
                .thenReturn(new ApiMessage("ERR-404", "USER_NOT_FOUND", "pt-BR", "Usuário {id} não achado", "Crie o usuário", 404));

        ApiException exception = new NotFoundException("USER_NOT_FOUND", Map.of("id", 10)); // Sem causa técnica (dispara log.warn)

        // Act
        ResponseEntity<ApiErrorResponse> responseEntity = handler.handle(exception, Locale.of("pt", "BR"), request);

        // Assert
        assertEquals(404, responseEntity.getStatusCode().value());
        ApiErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("ERR-404", body.code());
        assertEquals("Usuário 10 não achado", body.message());
        assertEquals("Crie o usuário", body.solution());
        assertEquals("/api/v1/users", body.path());
        assertNotNull(body.timestamp());
    }

    @Test
    void shouldHandleApiExceptionWithCauseAndFallbackUuid() {
        // Arrangeo (força UUID randômico)
        when(request.getRequestURI()).thenReturn("/api/v1/payments");

        when(resolver.resolve("PAYMENT_FAILED", Locale.US))
                .thenReturn(new ApiMessage("ERR-500", "PAYMENT_FAILED", "en", "Payment failed", "Retry later", 500));

        Throwable rootCause = new RuntimeException("Timeout na operadora do cartão");
        ApiException exception = new ApiException("PAYMENT_FAILED", Map.of(), rootCause); // Com causa técnica (dispara log.error)

        // Act
        ResponseEntity<ApiErrorResponse> responseEntity = handler.handle(exception, Locale.US, request);

        // Assert
        assertEquals(500, responseEntity.getStatusCode().value());
        ApiErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("/api/v1/payments", body.path());
    }

    @Test
    void shouldHandleApiMessageNotFoundExceptionInsideTryBlock() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        // Força o resolver a estourar a exceção de chave inexistente dentro do handle principal
        when(resolver.resolve("UNKNOWN_KEY", Locale.of("pt", "BR")))
                .thenThrow(new ApiMessageNotFoundException("UNKNOWN_KEY"));

        ApiException exception = new ApiException("UNKNOWN_KEY");

        // Act
        ResponseEntity<ApiErrorResponse> responseEntity = handler.handle(exception, Locale.of("pt", "BR"), request);

        // Assert
        assertEquals(500, responseEntity.getStatusCode().value());
        ApiErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("ERR-9999", body.code());
        assertEquals("Mensagem de API não encontrada.", body.message());
        assertEquals("/api/v1/orders", body.path());
    }

    @Test
    void shouldResolveValidationDetails() {
        when(request.getRequestURI()).thenReturn("/api/v1/accounts");

        Locale locale = Locale.of("pt", "BR");

        when(resolver.resolve("global.validation.failed", locale))
                .thenReturn(new ApiMessage(
                        "GLOBAL-0001",
                        "global.validation.failed",
                        "pt-BR",
                        "Um ou mais campos são inválidos.",
                        "Corrija os campos.",
                        400
                ));

        when(resolver.resolve("account.name.required", locale))
                .thenReturn(new ApiMessage(
                        "ACCOUNT-0101",
                        "account.name.required",
                        "pt-BR",
                        "O nome da conta é obrigatório.",
                        null,
                        400
                ));

        when(resolver.resolve("account.email.invalid", locale))
                .thenReturn(new ApiMessage(
                        "ACCOUNT-0110",
                        "account.email.invalid",
                        "pt-BR",
                        "O e-mail {0} está em um formato incorreto.",
                        null,
                        400
                ));

        ValidationException exception = new ValidationException(
                "global.validation.failed",
                List.of(
                        new ValidationDetail(
                                "name",
                                "account.name.required"
                        ),
                        new ValidationDetail(
                                "approvers[0].email",
                                "account.email.invalid",
                                Map.of("0", "invalido")
                        )
                )
        );

        ResponseEntity<ApiErrorResponse> responseEntity =
                handler.handle(exception, locale, request);

        assertEquals(400, responseEntity.getStatusCode().value());

        ApiErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals(2, body.details().size());
        assertEquals("name", body.details().get(0).field());
        assertEquals(
                "O nome da conta é obrigatório.",
                body.details().get(0).message()
        );
        assertEquals(
                "O e-mail invalido está em um formato incorreto.",
                body.details().get(1).message()
        );
    }

    @Test
    void shouldHandleDirectApiMessageNotFoundExceptionSignature() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        ApiMessageNotFoundException directException = new ApiMessageNotFoundException("DIRECT_MISSING_KEY");

        // Act
        // Testa o método anotado com o ExceptionHandler secundário direto
        ResponseEntity<ApiErrorResponse> responseEntity = handler.handleNotFound(directException, request);

        // Assert
        assertEquals(500, responseEntity.getStatusCode().value());
        ApiErrorResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("ERR-9999", body.code());
        assertEquals("/api/v1/products", body.path());
    }
}
