package br.com.portalmanager.platform.library.audit.model;

/** Technical snapshot strategy. This value is never persisted as business metadata. */
public enum AuditAction {
    CREATE,
    UPDATE,
    ACTIVATE,
    DEACTIVATE,
    RESTORE,
    DELETE,
    PURGE;

    public boolean capturesBefore() {
        return this == PURGE;
    }
}
