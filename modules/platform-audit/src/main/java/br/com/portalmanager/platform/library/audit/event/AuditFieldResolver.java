package br.com.portalmanager.platform.library.audit.event;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;

public final class AuditFieldResolver {

    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public AuditFieldResolver(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public Object resolve(ProceedingJoinPoint joinPoint, Object responseBody, AuditField field) {
        if (field == null || field.field().isBlank()) {
            return null;
        }

        return switch (field.source()) {
            case PATH -> resolvePathParameter(joinPoint, field.field());
            case BODY -> resolveObjectField(requestBody(joinPoint), field.field());
            case RESPONSE -> resolveObjectField(responseBody, field.field());
            case HEADER -> request.getHeader(field.field());
        };
    }

    private Object resolvePathParameter(ProceedingJoinPoint joinPoint, String fieldName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        String[] parameterNames = signature.getParameterNames();
        Object[] arguments = joinPoint.getArgs();

        for (int index = 0; index < arguments.length; index++) {
            if (hasPathVariableName(parameterAnnotations[index], fieldName)) {
                return arguments[index];
            }
            if (parameterNames != null && fieldName.equals(parameterNames[index])) {
                return arguments[index];
            }
        }
        return null;
    }

    private boolean hasPathVariableName(Annotation[] annotations, String fieldName) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof PathVariable pathVariable
                    && (fieldName.equals(pathVariable.name()) || fieldName.equals(pathVariable.value()))) {
                return true;
            }
        }
        return false;
    }

    private Object requestBody(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        Object[] arguments = joinPoint.getArgs();

        for (int index = 0; index < arguments.length; index++) {
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation instanceof RequestBody) {
                    return arguments[index];
                }
            }
        }
        return null;
    }

    private Object resolveObjectField(Object source, String fieldName) {
        if (source == null) {
            return null;
        }

        try {
            JsonNode node = objectMapper.valueToTree(source);
            JsonNode value = node.get(fieldName);
            return value == null || value.isNull() ? null : value.asText();
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
