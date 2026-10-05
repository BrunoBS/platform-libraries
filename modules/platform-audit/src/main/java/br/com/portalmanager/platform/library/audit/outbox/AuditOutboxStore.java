package br.com.portalmanager.platform.library.audit.outbox;

import tools.jackson.databind.JsonNode;

public interface AuditOutboxStore {

    void append(JsonNode payload, JsonNode metadata);
}
