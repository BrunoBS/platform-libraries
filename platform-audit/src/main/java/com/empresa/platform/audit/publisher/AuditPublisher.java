package com.empresa.platform.audit.publisher;

import com.empresa.platform.audit.model.AuditEventRequest;

public interface AuditPublisher {

    void publish(AuditEventRequest event);

    default void publishDirect(AuditEventRequest event) {
        publish(event);
    }
}
