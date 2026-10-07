package br.com.portalmanager.platform.library.audit.event;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import br.com.portalmanager.platform.library.audit.model.AuditMetadata;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;

public final class AuditEventFactory {

    private final PlatformAuditProperties properties;
    private final AuditAuthorizationContextResolver contextResolver;
    private final ObjectMapper objectMapper;

    public AuditEventFactory(
            PlatformAuditProperties properties,
            AuditAuthorizationContextResolver contextResolver,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.contextResolver = contextResolver;
        this.objectMapper = objectMapper;
    }

    public CapturedAuditEvent create(Auditable auditable, Object snapshot) {
        if (auditable.event().isBlank() || auditable.resourceType().isBlank()) {
            throw new AuditException(AuditMessageKeys.EVENT_DEFINITION_REQUIRED);
        }

        JsonNode payload;
        try {
            payload = objectMapper.valueToTree(snapshot);
        } catch (Exception exception) {
            throw new AuditException(AuditMessageKeys.EVENT_SERIALIZATION_FAILED, exception);
        }
        if (payload == null || payload.isNull() || !payload.isObject()) {
            throw new AuditException(AuditMessageKeys.SNAPSHOT_REQUIRED);
        }
        String resourceIdentifier = identifier(payload);
        if (resourceIdentifier == null || resourceIdentifier.isBlank()) {
            throw new AuditException(AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING);
        }

        AuditContext context = contextResolver.resolve();
        AuditMetadata metadata = new AuditMetadata(
                properties.getServiceName(),
                auditable.resourceType(),
                auditable.event(),
                resourceIdentifier,
                context.accountId(),
                context.applicationId(),
                context.environmentId(),
                context.correlationId(),
                context.actor(),
                Instant.now()
        );
        JsonNode metadataNode = toMetadataNode(metadata);
        return new CapturedAuditEvent(payload, metadataNode);
    }

    private String identifier(JsonNode payload) {
        JsonNode value = payload.get("identifier");
        if (value == null || value.isNull() || value.asText().isBlank()) {
            value = payload.get("id");
        }
        return value == null || value.isNull() ? null : value.asText();
    }

    private ObjectNode toMetadataNode(AuditMetadata metadata) {
        ObjectNode node = objectMapper.createObjectNode();
        putNullable(node, "service", metadata.service());
        putNullable(node, "resourceType", metadata.resourceType());
        putNullable(node, "eventType", metadata.eventType());
        putNullable(node, "resourceIdentifier", metadata.resourceIdentifier());
        putNullable(node, "accountIdentifier", metadata.accountIdentifier());
        putNullable(node, "applicationIdentifier", metadata.applicationIdentifier());
        putNullable(node, "environmentIdentifier", metadata.environmentIdentifier());
        putNullable(node, "correlationId", metadata.correlationId());
        putNullable(node, "username", metadata.username());
        node.put("occurredAt", metadata.occurredAt().toString());
        return node;
    }

    private void putNullable(ObjectNode node, String name, String value) {
        if (value == null) {
            node.putNull(name);
        } else {
            node.put(name, value);
        }
    }

    public record CapturedAuditEvent(JsonNode payload, JsonNode metadata) {
    }
}
