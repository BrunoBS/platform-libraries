package br.com.portalmanager.platform.library.testing.cloud.azure.internal.message;

import br.com.portalmanager.platform.library.messaging.api.model.PlatformErrorDefinition;

public final class AzureTestingTechnicalErrors {

    public static PlatformErrorDefinition invalidAzureConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-002",
                "Invalid platform-testing Azure configuration: " + detail,
                "Review the @WithAzureEmulator Blob Storage and Service Bus declarations.",
                500
        );
    }

    private AzureTestingTechnicalErrors() {
    }
}
