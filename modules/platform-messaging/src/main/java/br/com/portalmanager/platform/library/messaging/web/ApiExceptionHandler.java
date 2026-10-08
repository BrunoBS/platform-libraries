package br.com.portalmanager.platform.library.messaging.web;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ApiMessageNotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationDetailsProvider;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.message.PlatformTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.model.ApiErrorResponse;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.model.ApiValidationDetail;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import br.com.portalmanager.platform.library.messaging.resolver.DefaultApiMessageResolver;
import br.com.portalmanager.platform.library.messaging.message.MessageParameterResolver;
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
    private static final String STRUCTURED_ERROR_MDC_KEY = "platform.error";

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
        return resolve(exception, locale, request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException exception,
            Locale locale,
            HttpServletRequest request
    ) {
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

            return resolve(
                    new ValidationException(PlatformMessageKeys.REQUEST_FORMAT_INVALID),
                    locale,
                    request
            );
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
        var definition = PlatformTechnicalErrors.messageDefinitionNotFound(
                exception.getMessageKey()
        );
        ApiErrorResponse response = new ApiErrorResponse(
                definition.code(),
                definition.message(),
                definition.solution(),
                Instant.now(),
                request.getRequestURI(),
                MDC.get("correlationId")
        );

        logStructuredError(
                exception.getMessageKey(),
                response,
                exception,
                "Definição de mensagem não encontrada: " + definition.code()
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

            logResolvedException(exception, response);

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
        if (!(exception instanceof ValidationDetailsProvider validationException)
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
                    null,
                    detail.defaultMessage(),
                    null
            );
        }

        ApiMessage message;
        try {
            message = resolver.resolve(detail.messageKey(), locale);
            if (DefaultApiMessageResolver.DEFAULT_KEY.equals(message.messageKey())
                    && detail.fallbackMessageKey() != null
                    && !detail.fallbackMessageKey().isBlank()) {
                message = resolver.resolve(detail.fallbackMessageKey(), locale);
            }
        } catch (ApiMessageNotFoundException notFound) {
            if (detail.fallbackMessageKey() == null || detail.fallbackMessageKey().isBlank()) {
                throw notFound;
            }
            message = resolver.resolve(detail.fallbackMessageKey(), locale);
        }

        return new ApiValidationDetail(
                detail.field(),
                message.code(),
                MessageParameterResolver.resolve(
                        message.message(),
                        detail.parameters()
                ),
                MessageParameterResolver.resolve(
                        message.solution(),
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

    private void logResolvedException(
            ApiException exception,
            ApiErrorResponse response
    ) {
        String logMessage = exception.getCause() == null
                ? "Exceção de negócio resolvida: " + response.code()
                : "Erro de plataforma resolvido: " + response.code();

        logStructuredError(
                exception.getMessageKey(),
                response,
                exception.getCause(),
                logMessage
        );
    }

    private void logStructuredError(
            String messageKey,
            ApiErrorResponse response,
            Throwable cause,
            String logMessage
    ) {
        String previousStructuredError = MDC.get(STRUCTURED_ERROR_MDC_KEY);

        try {
            MDC.put(
                    STRUCTURED_ERROR_MDC_KEY,
                    toStructuredErrorJson(messageKey, response)
            );

            if (cause != null) {
                log.error(logMessage, cause);
            } else {
                log.warn(logMessage);
            }
        } finally {
            if (previousStructuredError == null) {
                MDC.remove(STRUCTURED_ERROR_MDC_KEY);
            } else {
                MDC.put(STRUCTURED_ERROR_MDC_KEY, previousStructuredError);
            }
        }
    }

    private String toStructuredErrorJson(
            String messageKey,
            ApiErrorResponse response
    ) {
        return "{" +
                "\"key\":" + jsonString(messageKey) + "," +
                "\"code\":" + jsonString(response.code()) + "," +
                "\"message\":" + jsonString(response.message()) + "," +
                "\"solution\":" + jsonString(response.solution()) + "," +
                "\"details\":" + toJsonDetails(response.details()) + "," +
                "\"timestamp\":" + jsonString(
                        response.timestamp() == null
                                ? null
                                : response.timestamp().toString()
             String toJsonDetails(List<ApiValidationDetail> details) {
        if (details == null || details.isEmpty()) {
            return "[]";
        }

        return details.stream()
                .map(detail -> "{" +
                        "\"field\":" + jsonString(detail.field()) + "," +
                        "\"code\":" + jsonString(detail.code()) + "," +
                        "\"message\":" + jsonString(detail.message()) + "," +
                        "\"solution\":" + jsonString(detail.solution()) +
                        "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

il.field()) + "," +
                        "\"message\":" + jsonString(detail.message()) +
                        "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escapeJson(value) + "\"";
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }

        return escaped.toString();
    }
}
