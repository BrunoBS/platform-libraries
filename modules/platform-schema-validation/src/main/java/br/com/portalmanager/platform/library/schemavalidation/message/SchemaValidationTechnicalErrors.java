package br.com.portalmanager.platform.library.schemavalidation.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class SchemaValidationTechnicalErrors {

    private static final String REQUIRED_VIEW_COLUMNS =
            "resource_type, resource_code, schema_version, definition";

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

    public static PlatformErrorDefinition schemaSourceUnavailable(String viewName) {
        return new PlatformErrorDefinition(
                "PLT-SCHEMA-005",
                "Schema Validation could not initialize because the schema source '" + viewName
                        + "' is unavailable. Expected columns: [" + REQUIRED_VIEW_COLUMNS
                        + "]. Fix: create/grant access to this view, configure platform.schema-validation.view-name "
                        + "with an existing compatible view, or provide a ResourceSchemaRepository implementation.",
                "Create or grant access to the configured schema view, override platform.schema-validation.view-name, "
                        + "or provide ResourceSchemaRepository.",
                500
        );
    }

    public static PlatformErrorDefinition schemaSourceContractInvalid(String type, String code) {
        return new PlatformErrorDefinition(
                "PLT-SCHEMA-006",
                "Schema source returned multiple schemas for " + type + "/" + code,
                "Ensure the schema source returns at most one published schema for each resource type and code.",
                500
        );
    }

    public static PlatformErrorDefinition schemaSourceMissing(String viewName) {
        return new PlatformErrorDefinition(
                "PLT-SCHEMA-007",
                "Schema Validation could not initialize because no schema source is available. The JDBC path expects view '"
                        + viewName + "' with columns [" + REQUIRED_VIEW_COLUMNS
                        + "]. Fix: configure the application datasource/JdbcTemplate and create this view, "
                        + "configure platform.schema-validation.view-name with an existing compatible view, "
                        + "or provide a ResourceSchemaRepository implementation.",
                "Configure the JDBC schema source or provide ResourceSchemaRepository.",
                500
        );
    }

    private SchemaValidationTechnicalErrors() {
    }
}
