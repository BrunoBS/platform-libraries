package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;

import java.util.ArrayList;
import java.util.List;

/** Builds and validates the complete event batch before any outbox row is written. */
public final class AuditInvocationEventFactory {

    private final AuditEventFactory eventFactory;

    public AuditInvocationEventFactory(AuditEventFactory eventFactory) {
        this.eventFactory = eventFactory;
    }

    public List<AuditEventFactory.CapturedAuditEvent> create(List<CapturedAuditSnapshot> snapshots) {
        List<AuditEventFactory.CapturedAuditEvent> events = new ArrayList<>(snapshots.size());
        for (CapturedAuditSnapshot snapshot : snapshots) {
            events.add(eventFactory.create(snapshot.auditable(), snapshot.value()));
        }
        return List.copyOf(events);
    }
}
