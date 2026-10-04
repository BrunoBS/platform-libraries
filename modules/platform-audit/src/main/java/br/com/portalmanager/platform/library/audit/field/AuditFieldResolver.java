package br.com.portalmanager.platform.library.audit.field;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditFieldResolver {

    private static final Set<String> SENSITIVE_NAME_PARTS = Set.of(
            "authorization", "cookie", "password", "passwd", "secret", "token",
            "apikey", "privatekey", "clientsecret", "credential", "jwt"
    );

    private final PlatformAuditProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public AuditFieldResolver(
            PlatformAuditProperties properties,
            ObjectMapper objectMapper,
            HttpServletRequest request
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResolvedFields resolve(Method method, Object[] arguments, Object responseBody, Auditable auditable) {
        JsonNode requestBody = requestBody(method, arguments);
        JsonNode response = null;
        boolean needsResponse = auditable.resourceId().source() == AuditFieldSource.RESPONSE
                || auditable.environment().source() == AuditFieldSource.RESPONSE
                || java.util.Arrays.stream(auditable.payload())
                .anyMatch(field -> field.source() == AuditFieldSource.RESPONSE);
        if (needsResponse) {
            response = toTree(responseBody);
        }
        boolean needsBody = auditable.resourceId().source() == AuditFieldSource.BODY
                || auditable.environment().source() == AuditFieldSource.BODY
                || java.util.Arrays.stream(auditable.payload())
                .anyMatch(field -> field.source() == AuditFieldSource.BODY);
        if (!needsBody) {
            requestBody = null;
        }

        String resourceId = stringify(resolveValue(method, arguments, response, requestBody, auditable.resourceId()));
        String environmentId = stringify(resolveValue(method, arguments, response, requestBody, auditable.environment()));
        Map<String, Object> payload = new LinkedHashMap<>();
        for (AuditField field : auditable.payload()) {
            if (field.field().isBlank()) {
                continue;
            }
            Object value = resolveValue(method, arguments, response, requestBody, field);
            if (value != null) {
                payload.put(field.field(), value);
            }
        }
        return new ResolvedFields(resourceId, environmentId, Map.copyOf(payload));
    }

    private Object resolveValue(
            Method method,
            Object[] arguments,
            JsonNode response,
            JsonNode body,
            AuditField field
    ) {
        if (field == null || field.field().isBlank()) {
            return null;
        }
        rejectSensitiveName(field.field());
        return switch (field.source()) {
            case PATH -> resolvePathParameter(method, arguments, field.field());
            case BODY -> read(body, field.field());
            case RESPONSE -> read(response, field.field());
            case HEADER -> resolveHeader(field.field());
        };
    }

    private Object resolveHeader(String name) {
        String normalized = normalize(name);
        if (isSensitive(normalized) || allowedHeaders().stream()
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
        return SENSITIVE_NAME_PARTS.stream().anyMatch(normalizedName::contains);
    }

    private Set<String> allowedHeaders() {
        Set<String> configured = properties.getAllowedHeaders();
        return configured == null ? Set.of() : configured;
    }

    private String normalize(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
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

    private JsonNode requestBody(Method method, Object[] arguments) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int index = 0; index < arguments.length; index++) {
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation instanceof RequestBody) {
                    return toTree(arguments[index]);
                }
            }
        }
        return null;
    }

    private JsonNode toTree(Object source) {
        if (source == null) {
            return null;
        }
        try {
            return objectMapper.valueToTree(source);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Object read(JsonNode source, String fieldName) {
        if (source == null) {
            return null;
        }
        JsonNode value = source.get(fieldName);
        if (value == null || value.isNull() || value.isContainerNode()) {
            return null;
        }
        return value.isTextual() ? value.asText() : value.numberValue() != null
                ? value.numberValue()
                : value.booleanValue();
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    public record ResolvedFields(String resourceId, String environmentId, Map<String, Object> payload) {
    }
}
