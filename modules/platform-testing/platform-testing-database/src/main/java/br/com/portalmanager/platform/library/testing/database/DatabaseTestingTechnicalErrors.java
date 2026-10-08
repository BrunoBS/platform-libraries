package br.com.portalmanager.platform.library.testing.database;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class DatabaseTestingTechnicalErrors {

    public static PlatformErrorDefinition invalidMySqlConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-004",
                "Invalid platform-testing MySQL configuration: " + detail,
                "Review the @WithMySql image declaration and use a versioned Docker image name.",
                500
        );
    }

    private DatabaseTestingTechnicalErrors() {
    }
}
