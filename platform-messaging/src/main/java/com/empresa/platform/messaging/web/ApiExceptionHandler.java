package com.empresa.platform.messaging.web;

import com.empresa.platform.messaging.config.PlatformMessagingProperties;
import com.empresa.platform.messaging.exception.ApiException;
import com.empresa.platform.messaging.exception.ApiMessageNotFoundException;
import com.empresa.platform.messaging.model.ApiErrorResponse;
import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.resolver.ApiMessageResolver;
import com.empresa.platform.messaging.util.MessageParameterResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Locale;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ApiMessageResolver resolver;
    private final PlatformMessagingProperties platformMessagingProperties;

    public ApiExceptionHandler(ApiMessageResolver resolver, PlatformMessagingProperties platformMessagingProperties) {
        this.resolver = resolver;
        this.platformMessagingProperties = platformMessagingProperties;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handle(ApiException e, Locale locale, HttpServletRequest request) {
        // GERAÇÃO DE LOGS: Expõe no log do servidor o erro do cliente e sua causa raiz (se houver)
        if (e.getCause() != null) {
            log.error("Erro de plataforma capturado para a chave '{}'. Causa raiz identificada: ", e.getMessageKey(), e.getCause());
        } else {
            log.warn("Exceção de negócio disparada sem causa raiz técnica para a chave '{}'.", e.getMessageKey());
        }
        try {
            ApiMessage m = resolver.resolve(e.getMessageKey(), locale);
            String messageResolved = MessageParameterResolver.resolve(m.message(), e.getParameters());
            String solutionResolved = MessageParameterResolver.resolve(m.solution(), e.getParameters());

            ApiErrorResponse response = new ApiErrorResponse(
                    m.code(),
                    messageResolved,
                    solutionResolved,
                    Instant.now(),
                    request.getRequestURI()
            );

            return ResponseEntity.status(m.httpStatus()).body(response);

        } catch (ApiMessageNotFoundException ex) {
            return handleNotFound(ex, request);
        }
    }

    @ExceptionHandler(ApiMessageNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ApiMessageNotFoundException e, HttpServletRequest request) {
        log.error("Catálogo de mensagens não encontrou uma definição para a chave '{}'.",
                e.getMessageKey());

        ApiErrorResponse fallbackResponse = new ApiErrorResponse(
                "ERR-9999",
                "Mensagem de API não encontrada.",
                "Verifique se a chave está cadastrada no catálogo de mensagens.",
                Instant.now(),
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(fallbackResponse);
    }


}
