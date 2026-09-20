package br.com.portalmanager.core.audit.publisher;

import br.com.portalmanager.core.audit.model.AuditEventRequest;

public interface AuditPublisher {

    void publish(AuditEventRequest event);

    default void publishDirect(AuditEventRequest event) {
        publish(event);
    }
}
