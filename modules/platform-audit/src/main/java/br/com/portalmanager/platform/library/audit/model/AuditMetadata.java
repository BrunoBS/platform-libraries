package br.com.portalmanager.platform.library.audit.model;

import java.time.Instant;

/** Context that lets asynchronous processing interpret a captured snapshot. */
public record AuditMetadata(
        String service,
        String resourceType,
        String eventType,
        String resourceIdentifier,
        String accountIdentifier,
        String applicationIdentifier,
        String environmentIdentifier,
        String correlationId,
        String username,
        Instant occurredAt
) {
}
