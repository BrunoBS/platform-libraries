package br.com.portalmanager.platform.library.testing.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class PlatformTestingTechnicalErrors {

    public static PlatformErrorDefinition invalidAwsConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-001",
                "Invalid platform-testing AWS configuration: " + detail,
                "Review the @WithAwsLocalStack S3 bucket, SQS queue, and notification declarations.",
                500
        );
    }

    public static PlatformErrorDefinition invalidAzureConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-002",
                "Invalid platform-testing Azure configuration: " + detail,
                "Review the @WithAzureEmulator Blob Storage and Service Bus declarations.",
                500
        );
    }

    private PlatformTestingTechnicalErrors() {
    }
}
