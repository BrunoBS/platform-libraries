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

    private PlatformTechnicalErrors() {
    }
}
