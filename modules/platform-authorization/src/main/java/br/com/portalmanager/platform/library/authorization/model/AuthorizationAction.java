package br.com.portalmanager.platform.library.authorization.model;

/** Transport-independent operation being authorized. */
public enum AuthorizationAction {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    ACTIVATE,
    DEACTIVATE,
    RESTORE
}
