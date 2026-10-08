package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AzureTestingTechnicalErrors {

    public static PlatformErrorDefinition invalidAzureConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-002",
                "Invalid platform-testing Azure configuration: " + detail,
                "undefined",
                500
        );
    }

    private AzureTestingTechnicalErrors() {
    }
}
