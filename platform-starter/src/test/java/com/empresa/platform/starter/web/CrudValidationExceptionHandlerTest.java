package com.empresa.platform.starter.web;

import com.empresa.platform.crud.validation.CrudValidationException;
import com.empresa.platform.crud.validation.CrudValidationResult;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.model.ApiErrorResponse;
import com.empresa.platform.messaging.web.ApiExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrudValidationExceptionHandlerTest {

    @Test
    void shouldAdaptCrudValidationExceptionToMessagingValidationException() {
        ApiExceptionHandler apiExceptionHandler = mock(ApiExceptionHandler.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        CrudValidationExceptionHandler handler =
                new CrudValidationExceptionHandler(apiExceptionHandler);

        CrudValidationResult result = new CrudValidationResult();
        result.addError("name", "account.name.required");

        CrudValidationException exception =
                new CrudValidationException(result);

        Locale locale = Locale.forLanguageTag("pt-BR");
        ResponseEntity<ApiErrorResponse> expected =
                ResponseEntity.badRequest().build();

        when(apiExceptionHandler.handle(
                org.mockito.ArgumentMatchers.any(ValidationException.class),
                eq(locale),
                eq(request)
        )).thenReturn(expected);

        ResponseEntity<?> response =
                handler.handle(exception, locale, request);

        assertThat(response).isSameAs(expected);

        ArgumentCaptor<ValidationException> captor =
                ArgumentCaptor.forClass(ValidationException.class);

        verify(apiExceptionHandler).handle(
                captor.capture(),
                eq(locale),
                eq(request)
        );

        ValidationException adapted = captor.getValue();

        assertThat(adapted.getDetails())
                .singleElement()
                .satisfies(detail -> {
                    assertThat(detail.field()).isEqualTo("name");
                    assertThat(detail.messageKey())
                            .isEqualTo("account.name.required");
                });
    }
}
