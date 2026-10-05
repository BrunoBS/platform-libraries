package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.outbox.AuditBeforeSnapshotProvider;
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Aspect
@Order(200)
public final class AuditAspect {

    private final AuditEventFactory eventFactory;
    private final AuditOutboxStore outboxStore;
    private final AuditBeforeSnapshotProvider beforeSnapshotProvider;
    private final PlatformAuditProperties properties;

    public AuditAspect(
            AuditEventFactory eventFactory,
            AuditOutboxStore outboxStore,
            AuditBeforeSnapshotProvider beforeSnapshotProvider,
            PlatformAuditProperties properties
    ) {
        this.eventFactory = eventFactory;
        this.outboxStore = outboxStore;
        this.beforeSnapshotProvider = beforeSnapshotProvider;
        this.properties = properties;
    }

    @Around("@annotation(br.com.portalmanager.platform.library.audit.annotation.Auditable) || "
            + "@annotation(br.com.portalmanager.platform.library.audit.annotation.Auditables)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new AuditException(AuditMessageKeys.TRANSACTION_REQUIRED);
        }

        Method method = resolveMethod(joinPoint);
        Auditable[] annotations = method.getAnnotationsByType(Auditable.class);
        List<AuditEventFactory.CapturedAuditEvent> events = new ArrayList<>();
        List<BeforeCapture> beforeCaptures = new ArrayList<>();

        for (Auditable auditable : annotations) {
            ensureSupportedAction(auditable.action());
            if (auditable.action().capturesBefore()) {
                if (beforeSnapshotProvider == null) {
                    throw new AuditException(AuditMessageKeys.BEFORE_SNAPSHOT_PROVIDER_REQUIRED);
                }
                Object snapshot = beforeSnapshotProvider.capture(method, joinPoint.getArgs(), auditable);
                beforeCaptures.add(new BeforeCapture(auditable, snapshot));
            }
        }

        Object result = joinPoint.proceed();
        if (!isSuccessful(result)) {
            return result;
        }

        for (BeforeCapture capture : beforeCaptures) {
            events.add(eventFactory.create(capture.auditable(), capture.snapshot()));
        }

        Object responseBody = result instanceof ResponseEntity<?> response ? response.getBody() : result;
        Collection<?> snapshots = responseBody instanceof Collection<?> collection
                ? collection
                : java.util.Collections.singletonList(responseBody);

        long afterActionCount = java.util.Arrays.stream(annotations)
                .filter(auditable -> !auditable.action().capturesBefore())
                .count();
        long eventCount = beforeCaptures.size() + afterActionCount * snapshots.size();
        if (eventCount > properties.getMaxEventsPerInvocation()) {
            throw new AuditException(AuditMessageKeys.EVENT_COUNT_EXCEEDED);
        }

        for (Auditable auditable : annotations) {
            if (auditable.action().capturesBefore()) {
                continue;
            }
            for (Object snapshot : snapshots) {
                events.add(eventFactory.create(auditable, snapshot));
            }
        }

        // The complete invocation is validated before the first insert. All inserts still use
        // the caller's transaction, so a failure rolls back both domain writes and outbox rows.
        for (AuditEventFactory.CapturedAuditEvent event : events) {
            outboxStore.append(event.payload(), event.metadata());
        }
        return result;
    }

    private void ensureSupportedAction(AuditAction action) {
        if (action == AuditAction.CUSTOM) {
            throw new AuditException(AuditMessageKeys.CUSTOM_ACTION_NOT_CONFIGURED);
        }
    }

    private boolean isSuccessful(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            int status = response.getStatusCode().value();
            return status >= 200 && status < 300;
        }
        return true;
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        Method declaredMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method targetMethod = AopUtils.getMostSpecificMethod(declaredMethod, joinPoint.getTarget().getClass());
        return targetMethod.getAnnotationsByType(Auditable.class).length > 0 ? targetMethod : declaredMethod;
    }

    private record BeforeCapture(Auditable auditable, Object snapshot) { }
}
