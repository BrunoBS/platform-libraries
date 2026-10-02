package br.com.portalmanager.platform.library.schemavalidation.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class SchemaValidationTechnicalErrors {

    public static final PlatformErrorDefinition PAYLOAD_BINDING_INVALID =
            new PlatformErrorDefinition(
                    "PLT-SCHEMA-001",
                    "@ValidateResourceSchema requires exactly one @SchemaPayload parameter",
                    "Annotate exactly one use case parameter with @SchemaPayload.",
                    500
            );

    public static PlatformErrorDefinition requiredResolverArgument(String field) {
        return new PlatformErrorDefinition(
                "PLT-SCHEMA-002",
                field + " is required to resolve a resource schema",
                "Provide a non-blank resource type and resource code in @ValidateResourceSchema.",
                500
        );
    }

    public static PlatformErrorDefinition publishedSchemaNotFound(String type, String code) {
        return new PlatformErrorDefinition(
                "PLT-SCHEMA-003",
                "No published resource schema found for " + type + "/" + code,
                "Publish the requested resource schema or configure the fallback schema.",
                500
        );
    }

    public static PlatformErrorDefinition publishedSchemaInvalid(
            String type,
            String code,
            Integer version
    ) {
        return new PlatformErrorDefinition(
                "PLT-SCHEMA-004",
                "Published resource schema is invalid for " + type + "/" + code + ", version " + version,
                "Correct and republish the JSON Schema definition.",
                500
        );
    }

    private SchemaValidationTechnicalErrors() {
    }
}
