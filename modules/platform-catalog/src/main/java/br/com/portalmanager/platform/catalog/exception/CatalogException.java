package br.com.portalmanager.platform.catalog.exception;

import br.com.portalmanager.platform.messaging.exception.ApiException;

import java.util.Map;

public abstract class CatalogException extends ApiException {

    protected CatalogException(String code, Map<String, Object> parameters) {
        super(code, parameters);
    }

    public String getCode() {
        return getMessageKey();
    }
}
