package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.outbox.AuditBeforeSnapshotProvider;

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
            Object[] arguments,
            Auditable[] annotations
    ) {
        List<CapturedAuditSnapshot> snapshots = new ArrayList<>();
        for (Auditable annotation : annotations) {
            if (annotation.action().capturesBefore()) {
                requireBeforeSnapshotProvider();
                Object snapshot = beforeSnapshotProvider.capture(arguments);
                snapshots.add(new CapturedAuditSnapshot(annotation, snapshot));
            }
        }
        return snapshots;
    }

    public List<CapturedAuditSnapshot> captureAfter(Object result, Auditable[] annotations) {
        Collection<?> values = snapshots(result);
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

    private void requireBeforeSnapshotProvider() {
        if (beforeSnapshotProvider == null) {
            throw new AuditException(AuditMessageKeys.BEFORE_SNAPSHOT_PROVIDER_REQUIRED);
        }
    }

    private Collection<?> snapshots(Object responseBody) {
        return responseBody instanceof Collection<?> collection
                ? collection
                : Collections.singletonList(responseBody);
    }
}
