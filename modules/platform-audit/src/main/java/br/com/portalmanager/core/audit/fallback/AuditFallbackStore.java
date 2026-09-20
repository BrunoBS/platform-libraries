package br.com.portalmanager.core.audit.fallback;

import br.com.portalmanager.core.audit.model.AuditEventRequest;

public interface AuditFallbackStore {

    void save(AuditEventRequest event);

    AuditEventRequest peek();

    void removeHead();

    boolean hasPending();
}
