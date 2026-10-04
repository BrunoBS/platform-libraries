package br.com.portalmanager.platform.library.audit.model;

import br.com.portalmanager.platform.library.messagequeue.annotation.QueueMessage;

import java.time.Instant;
import java.util.Map;

@QueueMessage(type = "platform.audit.event", version = "1")
public record AuditEventRequest(
        String eventId,
        Instant timestamp,
        String service,
        String accountId,
        String applicationId,
        String environmentId,
        String resource,
        String resourceId,
        String action,
        String actor,
        String correlationId,
        Integer httpStatus,
        Object payload,
        Map<String, Object> metadata
) {
}
