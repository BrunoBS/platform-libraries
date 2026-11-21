package br.com.portalmanager.platform.library.audit.internal.event;

import br.com.portalmanager.platform.library.messaging.api.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import br.com.portalmanager.platform.library.audit.api.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.api.annotation.AuditAttribute;
import java.util.HashSet;
import java.util.Set;
import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.internal.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.internal.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditContext;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditMetadata;
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
            throw new ValidationException(AuditMessageKeys.EVENT_DEFINITION_REQUIRED);
        }

        JsonNode payload;
        try {
            payload = objectMapper.valueToTree(snapshot);
        } catch (Exception exception) {
            throw new ApiException(AuditMessageKeys.EVENT_SERIALIZATION_FAILED, exception);
        }
        if (payload == null || payload.isNull() || !payload.isObject()) {
            throw new ValidationException(AuditMessageKeys.SNAPSHOT_REQUIRED);
        }
        ObjectNode attributes = mappedAttributes(auditable, payload);
        JsonNode identifier = attributes.get("resourceIdentifier");
        String resourceIdentifier = identifier == null || identifier.isNull() ? null : identifier.asText();
        if (resourceIdentifier == null || resourceIdentifier.isBlank()) {
            throw new ValidationException(AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING);
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
                Instant.now(),
                resourceVersion(attributes.get("resourceVersion"))
        );
        ObjectNode metadataNode = toMetadataNode(metadata);
        metadataNode.setAll(attributes);
        // Canonical identity fields keep their established String/Long types.
        metadataNode.put("resourceIdentifier", metadata.resourceIdentifier());
        if (attributes.has("resourceVersion") && metadata.resourceVersion() != null) {
            metadataNode.put("resourceVersion", metadata.resourceVersion().longValue());
        }
        return new CapturedAuditEvent(payload, metadataNode);
    }

    private ObjectNode mappedAttributes(Auditable auditable, JsonNode payload) {
        ObjectNode attributes = objectMapper.createObjectNode();
        Set<String> keys = new HashSet<>();
        Set<String> contextKeys = Set.of("service", "resourceType", "eventType", "accountIdentifier",
                "applicationIdentifier", "environmentIdentifier", "correlationId", "username", "occurredAt");
        for (AuditAttribute mapping : auditable.attributes()) {
            if (mapping.key().isBlank() || mapping.value().isBlank()
                    || contextKeys.contains(mapping.key()) || !keys.add(mapping.key())) {
                throw new ValidationException(AuditMessageKeys.ATTRIBUTE_MAPPING_INVALID);
            }
            JsonNode value = payload.get(mapping.value());
            if (value != null) attributes.set(mapping.key(), value.deepCopy());
        }
        return attributes;
    }

    private Long resourceVersion(JsonNode version) {
        if (version == null || version.isNull()) return null;
        if (!version.isIntegralNumber() || !version.canConvertToLong()) {
            throw new ValidationException(AuditMessageKeys.RESOURCE_VERSION_INVALID);
        }
        return version.longValue();
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
