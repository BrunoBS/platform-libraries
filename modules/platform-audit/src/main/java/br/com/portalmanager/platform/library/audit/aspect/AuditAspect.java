package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Order(200)
public final class AuditAspect {

    private final AuditSnapshotCollector snapshotCollector;
    private final AuditInvocationEventFactory eventFactory;
    private final AuditOutboxAppender outboxAppender;

    public AuditAspect(
            AuditSnapshotCollector snapshotCollector,
            AuditInvocationEventFactory eventFactory,
            AuditOutboxAppender outboxAppender
    ) {
        this.snapshotCollector = snapshotCollector;
        this.eventFactory = eventFactory;
        this.outboxAppender = outboxAppender;
    }

    @Around("@annotation(br.com.portalmanager.platform.library.audit.annotation.Auditable) || "
            + "@annotation(br.com.portalmanager.platform.library.audit.annotation.Auditables)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        requireActiveTransaction();

        Method method = resolveMethod(joinPoint);
        Auditable[] annotations = method.getAnnotationsByType(Auditable.class);
        List<CapturedAuditSnapshot> snapshots = new ArrayList<>(
                snapshotCollector.captureBefore(joinPoint.getArgs(), annotations)
        );

        Object result = joinPoint.proceed();
        snapshots.addAll(snapshotCollector.captureAfter(result, annotations));
        for (AuditEventFactory.CapturedAuditEvent event : eventFactory.create(snapshots)) {
            outboxAppender.append(event.payload(), event.metadata());
        }
        return result;
    }

    private void requireActiveTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new AuditException(AuditMessageKeys.TRANSACTION_REQUIRED);
        }
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        Method declaredMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method targetMethod = AopUtils.getMostSpecificMethod(declaredMethod, joinPoint.getTarget().getClass());
        return targetMethod.getAnnotationsByType(Auditable.class).length > 0 ? targetMethod : declaredMethod;
    }
}
