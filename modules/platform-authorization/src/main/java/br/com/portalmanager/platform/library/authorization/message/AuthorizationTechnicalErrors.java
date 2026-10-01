package br.com.portalmanager.platform.library.authorization.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AuthorizationTechnicalErrors {

    public static final PlatformErrorDefinition SERVICE_URL_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUTH-001",
                    "platform.authorization.service-url is required when platform.authorization.mode=REAL",
                    "Configure platform.authorization.service-url with the authorization service URL.",
                    500
            );

    public static final PlatformErrorDefinition SERVICE_UNAVAILABLE =
            new PlatformErrorDefinition(
                    "PLT-AUTH-002",
                    "Authorization API is unavailable.",
                    "Verify the Authorization API availability and connectivity.",
                    503
            );

    private AuthorizationTechnicalErrors() {
    }
}
