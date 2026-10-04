package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Collection;

@Aspect
public final class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final PlatformAuditProperties properties;
    private final AuditEventFactory eventFactory;
    private final AuditPublisher publisher;

    public AuditAspect(
            PlatformAuditProperties properties,
            AuditEventFactory eventFactory,
            AuditPublisher publisher
    ) {
        this.properties = properties;
        this.eventFactory = eventFactory;
        this.publisher = publisher;
    }

    @Around("@annotation(br.com.portalmanager.platform.library.audit.annotation.Auditable) || "
            + "@annotation(br.com.portalmanager.platform.library.audit.annotation.Auditables)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        Integer status = resolveStatus(result);
        if (status < 200 || status >= 300) {
            return result;
        }

        Object responseBody = result instanceof ResponseEntity<?> response
                ? response.getBody()
                : result;

        Method method = resolveMethod(joinPoint);
        for (Auditable auditable : method.getAnnotationsByType(Auditable.class)) {
            publishForResult(joinPoint, auditable, responseBody, status);
        }
        return result;
    }

    private void publishForResult(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object responseBody,
            Integer status
    ) {
        if (responseBody instanceof Collection<?> collection) {
            for (Object item : collection) {
                publish(joinPoint, auditable, item, status);
            }
            return;
        }
        publish(joinPoint, auditable, responseBody, status);
    }

    private void publish(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object responseBody,
            Integer status
    ) {
        AuditEventRequest event;
        try {
            event = eventFactory.create(joinPoint, auditable, responseBody, status);
        } catch (AuditException exception) {
            if (properties.isFailOnError()) {
                throw exception;
            }
            log.error(
                    "Audit event skipped | resource={} | action={}",
                    auditable.resource(),
                    auditable.action(),
                    exception
            );
            return;
        }

        publisher.publish(event);
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method declaredMethod = signature.getMethod();
        Method targetMethod = AopUtils.getMostSpecificMethod(
                declaredMethod,
                joinPoint.getTarget().getClass()
        );

        if (targetMethod.getAnnotationsByType(Auditable.class).length > 0) {
            return targetMethod;
        }
        return declaredMethod;
    }

    private Integer resolveStatus(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            return response.getStatusCode().value();
        }
        return 200;
    }
}
