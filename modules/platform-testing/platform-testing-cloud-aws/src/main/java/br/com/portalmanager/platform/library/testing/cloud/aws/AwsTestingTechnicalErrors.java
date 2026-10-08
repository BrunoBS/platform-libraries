package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AwsTestingTechnicalErrors {

    public static PlatformErrorDefinition invalidAwsConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-001",
                "Invalid platform-testing AWS configuration: " + detail,
                "Review the @WithAwsLocalStack S3 bucket, SQS queue, and notification declarations.",
                500
        );
    }

    private AwsTestingTechnicalErrors() {
    }
}
