package br.com.portalmanager.platform.library.audit.model;

/** Technical audit action; all snapshots come from the use-case return value. This value is never persisted as business metadata. */
public enum AuditAction {
    CREATE,
    UPDATE,
    ACTIVATE,
    DEACTIVATE,
    RESTORE,
    DELETE,
    PURGE;
}
