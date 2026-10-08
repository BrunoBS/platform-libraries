package br.com.portalmanager.platform.library.schemavalidation.integration;

import br.com.portalmanager.platform.library.messaging.cache.NoOpApiMessageCache;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.resolver.DefaultApiMessageResolver;
import br.com.portalmanager.platform.library.messaging.web.ApiExceptionHandler;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchemaValidationMessagingIntegrationTest {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String FUTURE_KEY = "schemavalidation.future-keyword";

    @Test
    void shouldResolveFutureSchemaKeywordFromMessagingRepositoryWithoutLibraryChange() {
        ApiMessageRepository repository = (key, locale) -> {
            if (PlatformMessageKeys.VALIDATION_FAILED.equals(key)) {
                return Optional.of(message(
                        "PLATFORM-VALIDATION",
                        key,
                        "Falha de validação.",
                        400
                ));
            }
            if (FUTURE_KEY.equals(key)) {
                return Optional.of(message(
                        "SCHEMA-DYNAMIC-0001",
                        key,
                        "Mensagem dinâmica para {0}.",
                        400
                ));
            }
            return Optional.empty();
        };

        var response = handler(repository).handle(
                validationException(),
                PT_BR,
                request()
        );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);
        assertThat(response.getBody().details().getFirst().message())
                .isEqualTo("Mensagem dinâmica para name.");
    }

    @Test
    void shouldResolveGenericSchemaFallbackWhenFutureKeywordIsNotRegistered() {
        ApiMessageRepository repository = (key, locale) -> {
            if (PlatformMessageKeys.VALIDATION_FAILED.equals(key)) {
                return Optional.of(message(
                        "PLATFORM-VALIDATION",
                        key,
                        "Falha de validação.",
                        400
                ));
            }
            if (SchemaValidationMessageKeys.INVALID.equals(key)) {
                return Optional.of(message(
                        "SCHEMA-VALIDATION-0001",
                        key,
                        "Schema inválido para {0}.",
                        400
                ));
            }
            return Optional.empty();
        };

        var response = handler(repository).handle(
                validationException(),
                PT_BR,
                request()
        );

        assertThat(response.getStatusCode().value())
                .isEqualTo(400);
        assertThat(response.getBody().details().getFirst().message())
                .isEqualTo("Schema inválido para name.");
    }

    private ApiExceptionHandler handler(ApiMessageRepository repository) {
        return new ApiExceptionHandler(
                new DefaultApiMessageResolver(
                        repository,
                        new NoOpApiMessageCache(),
                        PT_BR
                )
        );
    }

    private ValidationException validationException() {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(
                        new ValidationDetail(
                                "name",
                                FUTURE_KEY,
                                Map.of("0", "name"),
                                SchemaValidationMessageKeys.INVALID
                        )
                )
        );
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Accept-Language")).thenReturn("pt-BR");
        when(request.getLocale()).thenReturn(PT_BR);
        when(request.getRequestURI()).thenReturn("/test/schema");
        return request;
    }

    private ApiMessage message(String code, String key, String text, int status) {
        return new ApiMessage(
                code,
                key,
                PT_BR.toLanguageTag(),
                text,
                "Corrija o payload.",
                status
        );
    }
}
