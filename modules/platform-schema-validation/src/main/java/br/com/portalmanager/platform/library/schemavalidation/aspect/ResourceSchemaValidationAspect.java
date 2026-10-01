package br.com.portalmanager.platform.library.schemavalidation.aspect;

import br.com.portalmanager.platform.library.schemavalidation.annotation.SchemaPayload;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
public class ResourceSchemaValidationAspect {

    private final ResourceSchemaValidator validator;
    private final ObjectMapper objectMapper;

    public ResourceSchemaValidationAspect(
            ResourceSchemaValidator validator,
            ObjectMapper objectMapper
    ) {
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(binding)")
    public Object validate(
            ProceedingJoinPoint joinPoint,
            ValidateResourceSchema binding
    ) throws Throwable {
        Object payload = resolvePayload(joinPoint);
        JsonNode payloadNode = payload == null ? null : objectMapper.valueToTree(payload);
        removeNullObjectProperties(payloadNode);

        validator.validate(binding.type(), binding.code(), payloadNode);

        return joinPoint.proceed();
    }

    private void removeNullObjectProperties(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            java.util.List<String> nullFields = new java.util.ArrayList<>();
            objectNode.properties().forEach(entry -> {
                if (entry.getValue() == null || entry.getValue().isNull()) {
                    nullFields.add(entry.getKey());
                } else {
                    removeNullObjectProperties(entry.getValue());
                }
            });
            nullFields.forEach(objectNode::remove);
            return;
        }
        if (node != null && node.isArray()) {
            node.forEach(this::removeNullObjectProperties);
        }
    }

    private Object resolvePayload(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] arguments = joinPoint.getArgs();

        Object payload = null;
        int payloadCount = 0;

        for (int index = 0; index < parameterAnnotations.length; index++) {
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation.annotationType().equals(SchemaPayload.class)) {
                    payload = arguments[index];
                    payloadCount++;
                }
            }
        }

        if (payloadCount != 1) {
            throw new IllegalStateException(
                    "@ValidateResourceSchema requires exactly one @SchemaPayload parameter"
            );
        }

        return payload;
    }
}
