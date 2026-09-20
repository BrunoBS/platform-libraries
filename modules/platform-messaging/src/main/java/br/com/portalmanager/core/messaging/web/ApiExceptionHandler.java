package br.com.portalmanager.core.messaging.web;

import br.com.portalmanager.core.messaging.exception.ApiException;
import br.com.portalmanager.core.messaging.exception.ApiMessageNotFoundException;
import br.com.portalmanager.core.messaging.exception.ConflictException;
import br.com.portalmanager.core.messaging.exception.NotFoundException;
import br.com.portalmanager.core.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.core.messaging.exception.ValidationException;
import br.com.portalmanager.core.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.core.messaging.model.ApiErrorResponse;
import br.com.portalmanager.core.messaging.model.ApiMessage;
import br.com.portalmanager.core.messaging.model.ApiValidationDetail;
import br.com.portalmanager.core.messaging.model.ValidationDetail;
import br.com.portalmanager.core.messaging.resolver.ApiMessageResolver;
import br.com.portalmanager.core.messaging.util.MessageParameterResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ApiMessageResolver resolver;

    public ApiExceptionHandler(ApiMessageResolver resolver) {
        this.resolver = resolver;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handle(
            ApiException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        logException(exception);
        return resolve(exception, locale, request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        log.warn("Optimistic locking conflict: {}", exception.getMessage());
        return resolve(
                new ResourceVersionConflictException(exception),
                locale,
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        log.error(
                "Database integrity violation: {}",
                exception.getMostSpecificCause().getMessage(),
                exception
        );

        return resolve(
                new ConflictException(
                        PlatformMessageKeys.DATA_INTEGRITY,
                        exception
                ),
                locale,
                request
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(
            DataAccessException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        String errorId = UUID.randomUUID().toString();
        log.error("Database access error. Error ID: {}", errorId, exception);

        return resolve(
                new ApiException(
                        PlatformMessageKeys.INTERNAL_SERVER_ERROR,
                        Map.of("0", errorId),
                        exception
                ),
                locale,
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        return resolve(
                new NotFoundException(PlatformMessageKeys.RESOURCE_NOT_FOUND),
                locale,
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        String field = exception.getName();
        String requiredType = exception.getRequiredType() == null
                ? "unknown"
                : exception.getRequiredType().getSimpleName();

        return resolve(
                new ValidationException(
                        PlatformMessageKeys.VALIDATION_FAILED,
                        List.of(
                                new ValidationDetail(
                                        field,
                                        PlatformMessageKeys.TYPE_MISMATCH,
                                        Map.of(
                                                "0", field,
                                                "1", requiredType
                                        )
                                )
                        )
                ),
                locale,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(
            HttpMessageNotReadableException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        InvalidFormatException invalidFormat = findInvalidFormatException(exception);

        if (invalidFormat != null) {
            String field = invalidFormat.getPath().isEmpty()
                    ? "request"
                    : invalidFormat.getPath().getLast().getPropertyName();

            Class<?> targetType = invalidFormat.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                String allowedValues = Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));

                return resolve(
                        new ValidationException(
                                PlatformMessageKeys.VALIDATION_FAILED,
                                List.of(
                                        new ValidationDetail(
                                                field,
                                                PlatformMessageKeys.INVALID_ENUM,
                                                Map.of(
                                                        "0", field,
                                                        "1", allowedValues
                                                )
                                        )
                                )
                        ),
                        locale,
                        request
                );
            }
        }

        return resolve(
                new ValidationException(PlatformMessageKeys.REQUEST_NOT_READABLE),
                locale,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            Locale locale,
            HttpServletRequest request
    ) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error. Error ID: {}", errorId, exception);

        return resolve(
                new ApiException(
                        PlatformMessageKeys.INTERNAL_SERVER_ERROR,
                        Map.of("0", errorId),
                        exception
                ),
                locale,
                request
        );
    }

    @ExceptionHandler(ApiMessageNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ApiMessageNotFoundException exception,
            HttpServletRequest request
    ) {
        log.error(
                "Catálogo de mensagens não encontrou uma definição para a chave '{}'.",
                exception.getMessageKey()
        );

        ApiErrorResponse response = new ApiErrorResponse(
                "ERR-9999",
                "Mensagem de API não encontrada.",
                "Verifique se a chave está cadastrada no catálogo de mensagens.",
                Instant.now(),
                request.getRequestURI(),
                MDC.get("correlationId")
        );

        return ResponseEntity.internalServerError().body(response);
    }

    private ResponseEntity<ApiErrorResponse> resolve(
            ApiException exception,
            Locale locale,
            HttpServletRequest request
    ) {
        try {
            Locale requestLocale = resolveRequestLocale(request, locale);
            ApiMessage message = resolver.resolve(exception.getMessageKey(), requestLocale);

            ApiErrorResponse response = new ApiErrorResponse(
                    message.code(),
                    MessageParameterResolver.resolve(
                            message.message(),
                            exception.getParameters()
                    ),
                    MessageParameterResolver.resolve(
                            message.solution(),
                            exception.getParameters()
                    ),
                    resolveValidationDetails(exception, requestLocale),
                    Instant.now(),
                    request.getRequestURI(),
                    MDC.get("correlationId")
            );

            return ResponseEntity.status(message.httpStatus()).body(response);
        } catch (ApiMessageNotFoundException exceptionNotFound) {
            return handleNotFound(exceptionNotFound, request);
        }
    }

    private Locale resolveRequestLocale(HttpServletRequest request, Locale fallbackLocale) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }

        Locale requestLocale = request.getLocale();
        return requestLocale != null ? requestLocale : fallbackLocale;
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

    private InvalidFormatException findInvalidFormatException(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException) {
                return invalidFormatException;
            }
            current = current.getCause();
        }

        return null;
    }

    private void logException(ApiException exception) {
        if (exception.getCause() != null) {
            log.error(
                    "Erro de plataforma capturado para a chave '{}'.",
                    exception.getMessageKey(),
                    exception.getCause()
            );
            return;
        }

        log.warn(
                "Exceção de negócio disparada para a chave '{}'.",
                exception.getMessageKey()
        );
    }
}
