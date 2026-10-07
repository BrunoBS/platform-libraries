package br.com.portalmanager.platform.library.audit.outbox;

import tools.jackson.databind.JsonNode;

/** Supplies the persisted state immediately before a physical purge. */
@FunctionalInterface
public interface AuditBeforeSnapshotProvider {

    /**
     * Captures the persisted snapshot using the original arguments of the intercepted use case.
     *
     * @param sourceArguments arguments in the same order as the use-case method signature
     * @return the resource state before the purge
     */
    JsonNode capture(Object[] sourceArguments);
}
