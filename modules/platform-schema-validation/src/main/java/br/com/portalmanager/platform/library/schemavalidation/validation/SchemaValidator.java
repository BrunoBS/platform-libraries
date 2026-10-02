package br.com.portalmanager.platform.library.schemavalidation.validation;

import tools.jackson.databind.JsonNode;

/**
 * Public contract for validating a payload against a published resource schema.
 */
public interface SchemaValidator {

    void validate(String resourceType, String resourceCode, JsonNode payload);
}
