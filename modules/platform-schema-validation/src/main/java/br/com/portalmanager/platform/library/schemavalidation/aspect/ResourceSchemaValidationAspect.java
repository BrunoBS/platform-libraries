package br.com.portalmanager.platform.library.schemavalidation.aspect;

import br.com.portalmanager.platform.library.schemavalidation.annotation.SchemaPayload;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
public class ResourceSchemaValidationAspect {

    private final ResourceSchemaValidator validator;
    private final ObjectMapper objectMapper;
    private final Map<Method, Integer> payloadParameterIndexes = new ConcurrentHashMap<>();

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

        validator.validate(binding.type(), binding.code(), payloadNode);

        return joinPoint.proceed();
    }

    private Object resolvePayload(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        int payloadIndex = payloadParameterIndexes.computeIfAbsent(
                method,
                this::resolvePayloadParameterIndex
        );
        return joinPoint.getArgs()[payloadIndex];
    }

    private int resolvePayloadParameterIndex(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int payloadIndex = -1;
        int payloadCount = 0;

        for (int index = 0; index < parameterAnnotations.length; index++) {
            for (Annotation annotation : parameterAnnotations[index]) {
                if (annotation.annotationType().equals(SchemaPayload.class)) {
                    payloadIndex = index;
                    payloadCount++;
                }
            }
        }

        if (payloadCount != 1) {
            throw new PlatformConfigurationException(
                    SchemaValidationTechnicalErrors.PAYLOAD_BINDING_INVALID
            );
        }

        return payloadIndex;
    }
}
