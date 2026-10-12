package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Captures the snapshot returned by the use case, including the removed state for PURGE. */
public final class AuditSnapshotCollector {

    public List<CapturedAuditSnapshot> captureAfter(Object result, Auditable[] annotations) {
        Collection<?> values = result instanceof Collection<?> collection
                ? collection
                : Collections.singletonList(result);
        List<CapturedAuditSnapshot> snapshots = new ArrayList<>();
        for (Auditable annotation : annotations) {
            for (Object value : values) {
                snapshots.add(new CapturedAuditSnapshot(annotation, value));
            }
        }
        return snapshots;
    }
}
