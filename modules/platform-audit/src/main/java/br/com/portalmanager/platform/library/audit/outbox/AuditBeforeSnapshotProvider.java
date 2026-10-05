package br.com.portalmanager.platform.library.audit.outbox;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.Method;

/**
 * Supplies the persisted state immediately before a physical purge.
 * Applications may implement this when the use-case arguments only carry an identifier.
 */
@FunctionalInterface
public interface AuditBeforeSnapshotProvider {

    JsonNode capture(Method method, Object[] arguments, Auditable auditable);
}
