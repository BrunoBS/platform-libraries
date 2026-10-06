package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.audit.outbox.AuditBeforeSnapshotProvider;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Captures the resource state on the correct side of the use-case execution. */
public final class AuditSnapshotCollector {

    private final AuditBeforeSnapshotProvider beforeSnapshotProvider;

    public AuditSnapshotCollector(AuditBeforeSnapshotProvider beforeSnapshotProvider) {
        this.beforeSnapshotProvider = beforeSnapshotProvider;
    }

    public List<CapturedAuditSnapshot> captureBefore(
            Method method,
            Object[] arguments,
            Auditable[] annotations
    ) {
        validateActions(annotations);

        List<CapturedAuditSnapshot> snapshots = new ArrayList<>();
        for (Auditable annotation : annotations) {
            if (annotation.action().capturesBefore()) {
                requireBeforeSnapshotProvider();
                Object snapshot = beforeSnapshotProvider.capture(method, arguments, annotation);
                snapshots.add(new CapturedAuditSnapshot(annotation, snapshot));
            }
        }
        return snapshots;
    }

    public List<CapturedAuditSnapshot> captureAfter(Object result, Auditable[] annotations) {
        Object responseBody = responseBody(result);
        Collection<?> values = snapshots(responseBody);
        List<CapturedAuditSnapshot> snapshots = new ArrayList<>();

        for (Auditable annotation : annotations) {
            if (annotation.action().capturesBefore()) {
                continue;
            }
            for (Object value : values) {
                snapshots.add(new CapturedAuditSnapshot(annotation, value));
            }
        }
        return snapshots;
    }

    private void validateActions(Auditable[] annotations) {
        for (Auditable annotation : annotations) {
            if (annotation.action() == AuditAction.CUSTOM) {
                throw new AuditException(AuditMessageKeys.CUSTOM_ACTION_NOT_CONFIGURED);
            }
        }
    }

    private void requireBeforeSnapshotProvider() {
        if (beforeSnapshotProvider == null) {
            throw new AuditException(AuditMessageKeys.BEFORE_SNAPSHOT_PROVIDER_REQUIRED);
        }
    }

    private Object responseBody(Object result) {
        return result instanceof ResponseEntity<?> response ? response.getBody() : result;
    }

    private Collection<?> snapshots(Object responseBody) {
        return responseBody instanceof Collection<?> collection
                ? collection
                : Collections.singletonList(responseBody);
    }
}
