package br.com.portalmanager.platform.audit.aspect;

import br.com.portalmanager.platform.audit.annotation.AuditField;
import br.com.portalmanager.platform.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.audit.annotation.Auditable;
import br.com.portalmanager.platform.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.audit.model.AuditContext;
import br.com.portalmanager.platform.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.audit.publisher.AuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

@Aspect
public final class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final PlatformAuditProperties properties;
    private final AuditPublisher publisher;
    private final AuditAuthorizationContextResolver contextResolver;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    public AuditAspect(
            PlatformAuditProperties properties,
            AuditPublisher publisher,
            AuditAuthorizationContextResolver contextResolver,
            ObjectMapper objectMapper,
            HttpServletRequest request
    ) {
        this.properties = properties;
        this.publisher = publisher;
        this.contextResolver = contextResolver;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Around("@annotation(br.com.portalmanager.platform.audit.annotation.Auditable) || "
            + "@annotation(br.com.portalmanager.platform.audit.annotation.Auditables)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        Integer status = resolveStatus(result);
        if (status != null && (status < 200 || status >= 300)) {
            return result;
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Auditable[] auditables = method.getAnnotationsByType(Auditable.class);
        Object responseBody = result instanceof ResponseEntity<?> response
                ? response.getBody()
                : result;

        for (Auditable auditable : auditables) {
            if (responseBody instanceof Collection<?> collection) {
                for (Object item : collection) {
                    publish(joinPoint, auditable, item, status);
                }
            } else {
                publish(joinPoint, auditable, responseBody, status);
            }
        }

        return result;
    }

    private void publish(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object responseBody,
            Integer status
    ) {
        String resourceId = stringify(
                resolveField(joinPoint, responseBody, auditable.resourceId())
        );
        if (resourceId == null || resourceId.isBlank()) {
            return;
        }

        AuditContext context;
        try {
            context = contextResolver.resolve();
        } catch (RuntimeException exception) {
            if (properties.isFailOnError()) {
                throw exception;
            }
            log.error(
                    "Audit event skipped because no authorized UserContext is available | resource={} | resourceId={} | action={}",
                    auditable.resource(),
                    resourceId,
                    auditable.action(),
                    exception
            );
            return;
        }

        String environmentId = stringify(
                resolveField(joinPoint, responseBody, auditable.environment())
        );
        if (environmentId == null || environmentId.isBlank()) {
            environmentId = context.environmentId();
        }

        AuditEventRequest event = new AuditEventRequest(
                Instant.now(),
                properties.getServiceName(),
                context.accountId(),
                context.applicationId(),
                environmentId,
                auditable.resource(),
                resourceId,
                auditable.action(),
                context.actor(),
                context.correlationId(),
                status,
                responseBody,
                Map.of()
        );

        publisher.publish(event);
    }

    private Integer resolveStatus(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            return response.getStatusCode().value();
        }
        return 200;
    }

    private Object resolveField(
            ProceedingJoinPoint joinPoint,
            Object responseBody,
            AuditField field
    ) {
        if (field == null || field.field().isBlank()) {
            return null;
        }

        return switch (field.source()) {
            case PATH -> extractFromPath(joinPoint, field.field());
            case BODY -> extractFromObject(extractRequestBody(joinPoint), field.field());
            case RESPONSE -> extractFromObject(responseBody, field.field());
            case HEADER -> request.getHeader(field.field());
        };
    }

    private Object extractRequestBody(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] annotations = signature.getMethod().getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof RequestBody) {
                    return args[i];
                }
            }
        }
        return null;
    }

    private Object extractFromPath(ProceedingJoinPoint joinPoint, String fieldName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] names = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (names == null) {
            return null;
        }

        for (int i = 0; i < names.length; i++) {
            if (fieldName.equals(names[i])) {
                return args[i];
            }
        }
        return null;
    }

    private Object extractFromObject(Object source, String fieldName) {
        if (source == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }

        try {
            JsonNode node = objectMapper.valueToTree(source);
            JsonNode value = node.get(fieldName);
            return value != null && !value.isNull() ? value.asText() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
