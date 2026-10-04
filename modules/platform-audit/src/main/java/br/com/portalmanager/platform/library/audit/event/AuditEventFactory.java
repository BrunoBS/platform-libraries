package br.com.portalmanager.platform.library.audit.event;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import org.aspectj.lang.ProceedingJoinPoint;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class AuditEventFactory {

    private final PlatformAuditProperties properties;
    private final AuditAuthorizationContextResolver contextResolver;
    private final AuditFieldResolver fieldResolver;

    public AuditEventFactory(
            PlatformAuditProperties properties,
            AuditAuthorizationContextResolver contextResolver,
            AuditFieldResolver fieldResolver
    ) {
        this.properties = properties;
        this.contextResolver = contextResolver;
        this.fieldResolver = fieldResolver;
    }

    public AuditEventRequest create(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object responseBody,
            Integer status
    ) {
        String resourceId = stringify(fieldResolver.resolve(joinPoint, responseBody, auditable.resourceId()));
        if (resourceId == null || resourceId.isBlank()) {
            throw new AuditException(AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING);
        }

        AuditContext context = contextResolver.resolve();
        String environmentId = stringify(fieldResolver.resolve(joinPoint, responseBody, auditable.environment()));
        if (environmentId == null || environmentId.isBlank()) {
            environmentId = context.environmentId();
        }

        return new AuditEventRequest(
                UUID.randomUUID().toString(),
                Instant.now(),
                properties.getServiceName(),
                context.accountId(),
                context.applicationId(),
                environmentId,
                auditable.resource(),
                resourceId,
                auditable.action(),
                context.actor(),
                context.correlationId(),
                status,
                resolvePayload(joinPoint, auditable, responseBody),
                Map.of()
        );
    }

    private Map<String, Object> resolvePayload(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            Object responseBody
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (AuditField field : auditable.payload()) {
            if (field.field().isBlank()) {
                continue;
            }

            Object value = fieldResolver.resolve(joinPoint, responseBody, field);
            if (value != null) {
                payload.put(field.field(), value);
            }
        }
        return Map.copyOf(payload);
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }
}
