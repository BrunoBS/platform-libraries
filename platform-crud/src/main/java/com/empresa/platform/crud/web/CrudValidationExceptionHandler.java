package com.empresa.platform.crud.web;

import com.empresa.platform.crud.validation.CrudValidationException;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import com.empresa.platform.messaging.model.ValidationDetail;
import com.empresa.platform.messaging.web.ApiExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CrudValidationExceptionHandler {

    private final ApiExceptionHandler apiExceptionHandler;

    public CrudValidationExceptionHandler(ApiExceptionHandler apiExceptionHandler) {
        this.apiExceptionHandler = apiExceptionHandler;
    }

    @ExceptionHandler(CrudValidationException.class)
    public ResponseEntity<?> handle(
            CrudValidationException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        ValidationException validationException = new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                exception.getDetails().stream()
                        .map(detail -> new ValidationDetail(
                                detail.field(),
                                detail.messageKey(),
                                detail.parameters(),
                                detail.defaultMessage()
                        ))
                        .toList()
        );

        return apiExceptionHandler.handle(validationException, locale, request);
    }
}
