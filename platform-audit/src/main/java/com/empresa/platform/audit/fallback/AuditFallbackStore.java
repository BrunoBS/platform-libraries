package com.empresa.platform.audit.fallback;

import com.empresa.platform.audit.model.AuditEventRequest;

public interface AuditFallbackStore {

    void save(AuditEventRequest event);

    AuditEventRequest peek();

    void removeHead();

    boolean hasPending();
}
