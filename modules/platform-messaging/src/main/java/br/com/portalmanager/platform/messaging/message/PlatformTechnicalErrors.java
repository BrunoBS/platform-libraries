package br.com.portalmanager.platform.messaging.message;

import br.com.portalmanager.platform.messaging.model.PlatformErrorDefinition;

public final class PlatformTechnicalErrors {

    public static final PlatformErrorDefinition APPLICATION_NAME_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-MSG-001",
                    "spring.application.name is required when platform.messaging is enabled",
                    "Configure spring.application.name with the service name.",
                    500
            );

    public static final PlatformErrorDefinition VIEW_NAME_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-MSG-002",
                    "platform.messaging.datasource.view-name is required when the messaging datasource is enabled",
                    "Configure platform.messaging.datasource.view-name with the SQL view used to resolve messages.",
                    500
            );

    public static PlatformErrorDefinition invalidSqlIdentifier(String value) {
        return new PlatformErrorDefinition(
                "PLT-MSG-003",
                "Invalid SQL identifier configured for platform messaging: " + value,
                "Use only letters, numbers and underscore in platform.messaging.datasource.view-name.",
                500
        );
    }

    public static PlatformErrorDefinition invalidMessageDefinition(String key, String reason) {
        return new PlatformErrorDefinition(
                "PLT-MSG-004",
                "Invalid platform message definition for key '" + key + "': " + reason,
                "Use the format code|httpStatus|message|solution and provide all required fields.",
                500
        );
    }

    public static PlatformErrorDefinition messageBundleLoadFailed(String resource) {
        return new PlatformErrorDefinition(
                "PLT-MSG-005",
                "Could not load platform message bundle: " + resource,
                "Check that the message bundle exists, is readable and contains valid UTF-8 properties.",
                500
        );
    }

    public static PlatformErrorDefinition invalidBundleName(String resource) {
        return new PlatformErrorDefinition(
                "PLT-MSG-006",
                "Invalid platform message bundle name: '" + resource + "'",
                "Use <namespace>_<locale>.properties or messages_<locale>.properties.",
                500
        );
    }

    public static PlatformErrorDefinition invalidBundleKey(String resource, String reason) {
        return new PlatformErrorDefinition(
                "PLT-MSG-007",
                "Invalid platform message key in bundle '" + resource + "': " + reason,
                "Use a non-blank local key without repeating the bundle namespace prefix.",
                500
        );
    }

    public static PlatformErrorDefinition duplicateBundleKey(
            String key,
            String locale,
            String firstResource,
            String secondResource
    ) {
        return new PlatformErrorDefinition(
                "PLT-MSG-008",
                "Duplicate platform message key detected: key='" + key
                        + "', locale='" + locale
                        + "', bundles=['" + firstResource + "', '" + secondResource + "']",
                "Keep exactly one definition for each global message key and locale.",
                500
        );
    }

    public static PlatformErrorDefinition messageDefinitionNotFound(String key) {
        return new PlatformErrorDefinition(
                "PLT-MSG-009",
                "Platform message definition was not found for key '" + key + "'",
                "Register the message key in the configured view or provide it in a platform message bundle.",
                500
        );
    }

    private PlatformTechnicalErrors() {
    }
}
