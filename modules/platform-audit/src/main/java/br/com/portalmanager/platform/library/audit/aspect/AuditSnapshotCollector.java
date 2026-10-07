package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.outbox.AuditBeforeSnapshotProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Captures the resource state on the correct side of the use-case execution. */
public final class AuditSnapshotCollector {

    private final AuditBeforeSnapshotProvider beforeSnapshotProvider;
    private final ObjectMapper objectMapper;

    public AuditSnapshotCollector(
            AuditBeforeSnapshotProvider beforeSnapshotProvider,
            ObjectMapper objectMapper
    ) {
        this.beforeSnapshotProvider = beforeSnapshotProvider;
        this.objectMapper = objectMapper;
    }

    public List<CapturedAuditSnapshot> captureBefore(
            Object[] arguments,
            Auditable[] annotations
    ) {
        List<CapturedAuditSnapshot> snapshots = new ArrayList<>();
        for (Auditable annotation : annotations) {
            if (annotation.action().capturesBefore()) {
                requireBeforeSnapshotProvider();
                String resourceIdentifier = resourceIdentifier(arguments);
                Object snapshot = beforeSnapshotProvider.capture(resourceIdentifier);
                snapshots.add(new CapturedAuditSnapshot(annotation, snapshot));
            }
        }
        return snapshots;
    }

    public List<CapturedAuditSnapshot> captureAfter(Object result, Auditable[] annotations) {
        Collection<?> values = snapshots(result);
        List<CapturedAuditSnapshot> snapshots = new ArrayList<>();

        for (Auditable annotation : annotations) {
            if (annotation.action().capturesBefore()) {
                continue;
            }
            for (Object value : values) {
                snapshots.add(new CapturedAuditSnapshot(annotation, value));
            }
        }
        return snapshots;
    }

    private String resourceIdentifier(Object[] arguments) {
        if (arguments != null) {
            for (Object argument : arguments) {
                if (argument == null) {
                    continue;
                }

                JsonNode input;
                try {
                    input = objectMapper.valueToTree(argument);
                } catch (Exception exception) {
                    throw new AuditException(AuditMessageKeys.EVENT_SERIALIZATION_FAILED, exception);
                }
                if (input == null || !input.isObject()) {
                    continue;
                }

                String identifier = identifier(input, "identifier");
                if (identifier == null || identifier.isBlank()) {
                    identifier = identifier(input, "id");
                }
                if (identifier != null && !identifier.isBlank()) {
                    return identifier;
                }
            }
        }
        throw new AuditException(AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING);
    }

    private String identifier(JsonNode input, String property) {
        JsonNode value = input.get(property);
        return value == null || value.isNull() ? null : value.asText();
    }

    private void requireBeforeSnapshotProvider() {
        if (beforeSnapshotProvider == null) {
            throw new AuditException(AuditMessageKeys.BEFORE_SNAPSHOT_PROVIDER_REQUIRED);
        }
    }

    private Collection<?> snapshots(Object responseBody) {
        return responseBody instanceof Collection<?> collection
                ? collection
                : Collections.singletonList(responseBody);
    }
}
