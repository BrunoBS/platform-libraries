package br.com.portalmanager.platform.library.audit.publisher;

import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;

public interface AuditPublisher {

    void publish(AuditEventRequest event);

    default void publishDirect(AuditEventRequest event) {
        publish(event);
    }
}
