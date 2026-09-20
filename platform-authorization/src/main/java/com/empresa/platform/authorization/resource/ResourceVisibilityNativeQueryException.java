package com.empresa.platform.authorization.resource;

/**
 * Raised when a protected native query cannot be safely constrained by the
 * current resource visibility strategy.
 */
public class ResourceVisibilityNativeQueryException extends RuntimeException {

    public ResourceVisibilityNativeQueryException(String message) {
        super(message);
    }
}
