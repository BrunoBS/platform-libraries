package com.empresa.platform.messaging.web;

import com.empresa.platform.messaging.config.PlatformMessagingProperties;
import com.empresa.platform.messaging.exception.ApiException;
import com.empresa.platform.messaging.exception.ApiMessageNotFoundException;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.model.ApiErrorResponse;
import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.model.ApiValidationDetail;
import com.empresa.platform.messaging.model.ValidationDetail;
import com.empresa.platform.messaging.resolver.ApiMessageResolver;
import com.empresa.platform.messaging.util.MessageParameterResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ApiMessageResolver resolver;
    private final PlatformMessagingProperties platformMessagingProperties;

    public ApiExceptionHandler(
            ApiMessageResolver resolver,
            PlatformMessagingProperties platformMessagingProperties
    ) {
        this.resolver = resolver;
        this.platformMessagingProperties = platformMessagingProperties;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            ApiException e,
            Locale locale,
            HttpServletRequest request
    ) {
        if (e.getCause() != null) {
            log.error(
                    "Erro de plataforma capturado para a chave '{}'. Causa raiz identificada: ",
                    e.getMessageKey(),
                    e.getCause()
            );
        } else {
            log.warn(
                    "Exceção de negócio disparada sem causa raiz técnica para a chave '{}'.",
                    e.getMessageKey()
            );
        }

        try {
            String correlationId = MDC.get("correlationId");
            ApiMessage message = resolver.resolve(e.getMessageKey(), locale);

            String resolvedMessage = MessageParameterResolver.resolve(
                    message.message(),
                    e.getParameters()
            );

            String resolvedSolution = MessageParameterResolver.resolve(
                    message.solution(),
                    e.getParameters()
            );

            List<ApiValidationDetail> details = resolveValidationDetails(e, locale);

            ApiErrorResponse response = new ApiErrorResponse(
                    message.code(),
                    resolvedMessage,
                    resolvedSolution,
                    details,
                    Instant.now(),
                    request.getRequestURI(),
                    correlationId
            );

            return ResponseEntity.status(message.httpStatus()).body(response);

        } catch (ApiMessageNotFoundException ex) {
            return handleNotFound(ex, request);
        }
    }

    @ExceptionHandler(ApiMessageNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ApiMessageNotFoundException e,
            HttpServletRequest request
    ) {
        log.error(
                "Catálogo de mensagens não encontrou uma definição para a chave '{}'.",
                e.getMessageKey()
        );

        String correlationId = MDC.get("correlationId");

        ApiErrorResponse fallbackResponse = new ApiErrorResponse(
                "ERR-9999",
                "Mensagem de API não encontrada.",
                "Verifique se a chave está cadastrada no catálogo de mensagens.",
                Instant.now(),
                request.getRequestURI(),
                correlationId
        );

        return ResponseEntity.internalServerError().body(fallbackResponse);
    }

    private List<ApiValidationDetail> resolveValidationDetails(
            ApiException exception,
            Locale locale
    ) {
        if (!(exception instanceof ValidationException validationException)
                || validationException.getDetails().isEmpty()) {
            return List.of();
        }

        return validationException.getDetails()
                .stream()
                .map(detail -> resolveValidationDetail(detail, locale))
                .toList();
    }

    private ApiValidationDetail resolveValidationDetail(
            ValidationDetail detail,
            Locale locale
    ) {
        if (detail.messageKey() == null || detail.messageKey().isBlank()) {
            return new ApiValidationDetail(
                    detail.field(),
                    detail.defaultMessage()
            );
        }

        ApiMessage message = resolver.resolve(detail.messageKey(), locale);

        return new ApiValidationDetail(
                detail.field(),
                MessageParameterResolver.resolve(
                        message.message(),
                        detail.parameters()
                )
        );
    }
}
