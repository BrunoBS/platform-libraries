package br.com.portalmanager.platform.library.catalog.exception;

import java.util.Map;

public class CatalogRestoreException extends CatalogException {

    public CatalogRestoreException(String code, Map<String, Object> parameters) {
        super(code, parameters);
    }
}
