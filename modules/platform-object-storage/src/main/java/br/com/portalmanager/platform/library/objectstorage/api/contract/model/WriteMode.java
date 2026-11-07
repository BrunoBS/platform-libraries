package br.com.portalmanager.platform.library.objectstorage.api.contract.model;

public enum WriteMode {
    /** Create only if no current object exists; otherwise upload fails with a conflict. */
    CREATE_ONLY,
    /** Replace the current object; if versioning is enabled, the provider may retain the previous version. */
    OVERWRITE
}
