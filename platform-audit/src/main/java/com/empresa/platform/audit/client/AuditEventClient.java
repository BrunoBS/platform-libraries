package com.empresa.platform.audit.client;

import com.empresa.platform.audit.model.AuditEventRequest;

public interface AuditEventClient {
    void publish(AuditEventRequest event);
}
