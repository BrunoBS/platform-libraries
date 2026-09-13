package com.empresa.platform.catalog.exception;

import java.util.Map;

public class CatalogNotFoundException extends CatalogException {

    public CatalogNotFoundException(String code, Map<String, Object> parameters) {
        super(code, parameters);
    }
}
