package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;

import java.util.ArrayList;
import java.util.List;

/** Builds and validates the complete event batch before any outbox row is written. */
public final class AuditInvocationEventFactory {

    private final AuditEventFactory eventFactory;
    private final PlatformAuditProperties properties;

    public AuditInvocationEventFactory(
            AuditEventFactory eventFactory,
            PlatformAuditProperties properties
    ) {
        this.eventFactory = eventFactory;
        this.properties = properties;
    }

    public List<AuditEventFactory.CapturedAuditEvent> create(List<CapturedAuditSnapshot> snapshots) {
        if (snapshots.size() > properties.getMaxEventsPerInvocation()) {
            throw new AuditException(AuditMessageKeys.EVENT_COUNT_EXCEEDED);
        }

        List<AuditEventFactory.CapturedAuditEvent> events = new ArrayList<>(snapshots.size());
        for (CapturedAuditSnapshot snapshot : snapshots) {
            events.add(eventFactory.create(snapshot.auditable(), snapshot.value()));
        }
        return List.copyOf(events);
    }
}
