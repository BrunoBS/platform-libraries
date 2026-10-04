package br.com.portalmanager.platform.library.audit.field;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.JsonNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AuditFieldResolver {

    private static final Set<String> SENSITIVE_NAME_PARTS = Set.of(
            "authorization", "cookie", "password", "passwd", "secret", "token",
            "apikey", "privatekey", "clientsecret", "credential", "jwt", "ssn",
            "taxid", "accesskey", "refresh", "bearer", "cardnumber", "creditcard", "pincode", "cvv", "cvc"
    );

    private final PlatformAuditProperties properties;
    private final HttpServletRequest request;

    public AuditFieldResolver(PlatformAuditProperties properties, HttpServletRequest request) {
        this.properties = properties;
        this.request = request;
    }

    public ResolvedFields resolve(Method method, Object[] arguments, Object responseBody, Auditable auditable) {
        Object requestBody = requestBody(method, arguments);
        String resourceId = stringify(resolveValue(
                method, arguments, responseBody, requestBody, auditable.resourceId()));
        String environmentId = stringify(resolveValue(
                method, arguments, responseBody, requestBody, auditable.environment()));

        Map<String, Object> payload = new LinkedHashMap<>();
        for (AuditField field : auditable.payload()) {
            if (field.field().isBlank()) {
                continue;
            }
            Object value = resolveValue(method, arguments, responseBody, requestBody, field);
            if (value != null) {
                payload.put(field.field(), value);
            }
        }
        return new ResolvedFields(resourceId, environmentId, Map.copyOf(payload));
    }

    private Object resolveValue(
            Method method,
            Object[] arguments,
            Object responseBody,
            Object requestBody,
            AuditField field
    ) {
        if (field == null || field.field().isBlank()) {
            return null;
        }
        rejectSensitiveName(field.field());
        return switch (field.source()) {
            case PATH -> resolvePathParameter(method, arguments, field.field());
            case BODY -> read(requestBody, field.field());
            case RESPONSE -> read(responseBody, field.field());
            case HEADER -> resolveHeader(field.field());
        };
    }

    private Object resolveHeader(String name) {
        String normalized = normalize(name);
        if (isSensitive(normalized) || allowedHeaders().stream()
                .filter(java.util.Objects::nonNull)
                .map(this::normalize)
                .noneMatch(normalized::equals)) {
            throw new AuditException(AuditMessageKeys.FIELD_NOT_ALLOWED);
        }
        return request.getHeader(name);
    }

    private void rejectSensitiveName(String name) {
        if (isSensitive(normalize(name))) {
            throw new AuditException(AuditMessageKeys.FIELD_NOT_ALLOWED);
        }
    }

    private boolean isSensitive(String normalizedName) {
        return normalizedName.equals("pin")
                || SENSITIVE_NAME_PARTS.stream().anyMatch(normalizedName::contains);
    }

    private Set<String> allowedHeaders() {
        Set<String> configured = properties.getAllowedHeaders();
        return configured == null ? Set.of() : configured;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private Object resolvePathParameter(Method method, Object[] arguments, String fieldName) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        var parameters = method.getParameters();
        for (int index = 0; index < arguments.length; index++) {
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation instanceof PathVariable pathVariable
                        && (fieldName.equals(pathVariable.name()) || fieldName.equals(pathVariable.value()))) {
                    return arguments[index];
                }
            }
            if (fieldName.equals(parameters[index].getName())) {
                return arguments[index];
            }
        }
        return null;
    }

    private Object requestBody(Method method, Object[] arguments) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int index = 0; index < arguments.length; index++) {
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation instanceof RequestBody) {
                    return arguments[index];
                }
            }
        }
        return null;
    }

    private Object read(Object source, String fieldName) {
        if (source == null) {
            return null;
        }
        try {
            if (source instanceof JsonNode node) {
                return scalar(node.get(fieldName));
            }
            Object value;
            if (source instanceof Map<?, ?> map) {
                value = map.get(fieldName);
            } else {
                BeanWrapperImpl bean = new BeanWrapperImpl(source);
                if (!bean.isReadableProperty(fieldName)) {
                    return null;
                }
                value = bean.getPropertyValue(fieldName);
            }
            return scalar(value);
        } catch (AuditException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuditException(AuditMessageKeys.FIELD_RESOLUTION_FAILED, exception);
        }
    }

    private Object scalar(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        if (value.isNumber()) {
            return value.numberValue();
        }
        return value.isBoolean() ? value.booleanValue() : null;
    }

    private Object scalar(Object value) {
        if (value instanceof JsonNode node) {
            return scalar(node);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof CharSequence || value instanceof Character
                || value instanceof Enum<?> || value instanceof UUID
                || value instanceof TemporalAccessor) {
            return value instanceof Enum<?> enumValue ? enumValue.name() : value.toString();
        }
        return null;
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    public record ResolvedFields(String resourceId, String environmentId, Map<String, Object> payload) {
    }
}
