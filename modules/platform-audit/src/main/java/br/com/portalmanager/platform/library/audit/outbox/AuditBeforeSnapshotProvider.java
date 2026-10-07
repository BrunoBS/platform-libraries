package br.com.portalmanager.platform.library.audit.outbox;

import tools.jackson.databind.JsonNode;

/** Supplies the persisted state immediately before a physical purge. */
@FunctionalInterface
public interface AuditBeforeSnapshotProvider {

    JsonNode capture(String resourceIdentifier);
}
