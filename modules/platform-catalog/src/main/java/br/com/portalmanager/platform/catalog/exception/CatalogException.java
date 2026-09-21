package br.com.portalmanager.platform.catalog.exception;

import java.util.Map;

public abstract class CatalogException extends RuntimeException {

    private final String code;
    private final Map<String, Object> parameters;

    protected CatalogException(String code, Map<String, Object> parameters) {
        super(code);
        this.code = code;
        this.parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}
