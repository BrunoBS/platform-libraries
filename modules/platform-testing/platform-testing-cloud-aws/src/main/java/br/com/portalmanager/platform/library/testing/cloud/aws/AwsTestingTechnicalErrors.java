package br.com.portalmanager.platform.library.testing.cloud.aws;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AwsTestingTechnicalErrors {

    public static PlatformErrorDefinition invalidAwsConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-001",
                "Invalid platform-testing AWS configuration: " + detail,
                "undefined",
                500
        );
    }

    private AwsTestingTechnicalErrors() {
    }
}
