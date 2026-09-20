package br.com.portalmanager.core.catalog.exception;

import java.util.Map;

public class CatalogRestoreException extends CatalogException {

    public CatalogRestoreException(String code, Map<String, Object> parameters) {
        super(code, parameters);
    }
}
