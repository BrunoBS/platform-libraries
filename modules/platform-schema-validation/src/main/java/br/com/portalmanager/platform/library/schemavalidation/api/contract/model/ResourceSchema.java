package br.com.portalmanager.platform.library.schemavalidation.api.contract.model;

public record ResourceSchema(
        String resourceType,
        String resourceCode,
        Integer schemaVersion,
        String definition
) {
}
