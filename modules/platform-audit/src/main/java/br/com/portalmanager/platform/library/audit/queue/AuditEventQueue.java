package br.com.portalmanager.platform.library.audit.queue;

import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;

public interface AuditEventQueue {
    void save(AuditEventRequest event);
    AuditEventRequest peek();
    void removeHead();
    boolean hasPending();
}
