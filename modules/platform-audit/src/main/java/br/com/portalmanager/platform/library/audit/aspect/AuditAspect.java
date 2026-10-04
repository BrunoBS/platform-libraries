package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

        Object responseBody = result instanceof ResponseEntity<?> response ? response.getBody() : result;
        Method method = resolveMethod(joinPoint);
        Auditable[] annotations = method.getAnnotationsByType(Auditable.class);
        Collection<?> items = responseBody instanceof Collection<?> collection
                ? collection
                : Collections.singletonList(responseBody);
        long eventCount = (long) annotations.length * items.size();
        if (eventCount > properties.getMaxEventsPerInvocation()) {
            handleAuditException(
                    new AuditException(AuditMessageKeys.EVENT_COUNT_EXCEEDED),
                    annotations.length == 0 ? null : annotations[0]
            );
            return result;
        }

        List<AuditEventRequest> events = new ArrayList<>();
        for (Auditable auditable : annotations) {
            for (Object item : items) {
                try {
                    events.add(eventFactory.create(method, joinPoint.getArgs(), auditable, item, status));
                } catch (AuditException exception) {
                    handleAuditException(exception, auditable);
                }
            }
        }

        // Validate the complete batch before the first broker call. Broker failures can still
        // leave a partial batch; transactional delivery belongs in an outbox at the caller.
        for (AuditEventRequest event : events) {
            publisher.publish(event);
        }
        return result;
    }

    private void handleAuditException(AuditException exception, Auditable auditable) {
        if (properties.isFailOnError()) {
            throw exception;
        }
        log.error(
                "Audit event skipped | resource={} | action={}",
                auditable == null ? "unknown" : auditable.resource(),
                auditable == null ? "unknown" : auditable.action(),
                exception
        );
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
