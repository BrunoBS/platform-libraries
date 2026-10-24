package br.com.portalmanager.platform.library.schemavalidation.validation;

import tools.jackson.databind.JsonNode;

/**
 * Public contract for validating a payload against a published resource schema.
 */
public interface SchemaValidator {

    void validate(String resourceType, String resourceCode, JsonNode payload);

    /**
     * Validates when a published resource schema (or configured fallback) exists.
     * Returns false only when no schema is configured; validation and infrastructure errors still fail.
     */
    default boolean validateIfPresent(String resourceType, String resourceCode, JsonNode payload) {
        validate(resourceType, resourceCode, payload);
        return true;
    }
}
