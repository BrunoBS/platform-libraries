package br.com.portalmanager.platform.library.audit.event;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.field.AuditFieldResolver;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditEventFactory {

    private final PlatformAuditProperties properties;
    private final AuditAuthorizationContextResolver contextResolver;
    private final AuditFieldResolver fieldResolver;
    private final ObjectMapper objectMapper;

    public AuditEventFactory(
            PlatformAuditProperties properties,
            AuditAuthorizationContextResolver contextResolver,
            AuditFieldResolver fieldResolver,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.contextResolver = contextResolver;
        this.fieldResolver = fieldResolver;
        this.objectMapper = objectMapper;
    }

    public AuditEventRequest create(
            Method method,
            Object[] arguments,
            Auditable auditable,
            Object responseBody,
            Integer status
    ) {
        AuditFieldResolver.ResolvedFields fields =
                fieldResolver.resolve(method, arguments, responseBody, auditable);
        String resourceId = fields.resourceId();
        if (resourceId == null || resourceId.isBlank()) {
            throw new AuditException(AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING);
        }
        if (auditable.resource().isBlank() || auditable.action().isBlank()) {
            throw new AuditException(AuditMessageKeys.RESOURCE_ACTION_REQUIRED);
        }

        AuditContext context = contextResolver.resolve();
        String environmentId = fields.environmentId();
        if (environmentId == null || environmentId.isBlank()) {
            environmentId = context.environmentId();
        }

        AuditEventRequest event = new AuditEventRequest(
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
                fields.payload(),
                Map.of()
        );
        enforceSizeLimit(event);
        return event;
    }

    private void enforceSizeLimit(AuditEventRequest event) {
        try {
            int serializedSize = objectMapper.writeValueAsBytes(event).length;
            if (serializedSize > properties.getMaxEventSizeBytes()) {
                throw new AuditException(AuditMessageKeys.EVENT_TOO_LARGE);
            }
        } catch (AuditException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuditException(AuditMessageKeys.EVENT_SERIALIZATION_FAILED, exception);
        }
    }
}
