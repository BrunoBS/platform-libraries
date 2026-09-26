package br.com.portalmanager.platform.library.audit.fallback;

import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;

public interface AuditFallbackStore {

    void save(AuditEventRequest event);

    AuditEventRequest peek();

    void removeHead();

    boolean hasPending();
}
