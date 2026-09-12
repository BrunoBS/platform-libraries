package com.empresa.platform.messaging.web;

import com.empresa.platform.messaging.exception.ApiException;
import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.resolver.ApiMessageResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerLocaleTest {

    @Test
    void shouldUseAcceptLanguageFromRequest() {
        AtomicReference<Locale> resolvedLocale = new AtomicReference<>();
        ApiMessageResolver resolver = (key, locale) -> {
            resolvedLocale.set(locale);
            return message();
        };

        ApiExceptionHandler handler = new ApiExceptionHandler(resolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "en-US");

        handler.handle(new ApiException("test.message"), Locale.forLanguageTag("pt-BR"), request);

        assertThat(resolvedLocale.get()).isEqualTo(Locale.US);
    }

    @Test
    void shouldDelegateToConfiguredDefaultLocaleWhenAcceptLanguageIsAbsent() {
        AtomicReference<Locale> resolvedLocale = new AtomicReference<>();
        ApiMessageResolver resolver = (key, locale) -> {
            resolvedLocale.set(locale);
            return message();
        };

        ApiExceptionHandler handler = new ApiExceptionHandler(resolver);

        MockHttpServletRequest request = new MockHttpServletRequest();

        handler.handle(new ApiException("test.message"), Locale.forLanguageTag("pt-BR"), request);

        assertThat(resolvedLocale.get()).isNull();
    }

    private static ApiMessage message() {
        return new ApiMessage(
                "ERR-0001",
                HttpStatus.BAD_REQUEST.value(),
                "Mensagem",
                "Solução",
                "pt-BR"
        );
    }
}
