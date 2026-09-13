package com.empresa.platform.crud.exception;

public class CrudResourceNotFoundException extends RuntimeException {

    public CrudResourceNotFoundException(String resourceName, Object id) {
        super(resourceName + " not found for id " + id);
    }
}
