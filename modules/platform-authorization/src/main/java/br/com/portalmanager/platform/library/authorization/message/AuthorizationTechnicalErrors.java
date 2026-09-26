package br.com.portalmanager.platform.library.authorization.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AuthorizationTechnicalErrors {

    public static final PlatformErrorDefinition SERVICE_URL_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUTH-001",
                    "platform.authorization.service-url is required when platform.authorization is enabled",
                    "Configure platform.authorization.service-url with the authorization service URL.",
                    500
            );

    private AuthorizationTechnicalErrors() {
    }
}
