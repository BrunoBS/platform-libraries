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

    public static final PlatformErrorDefinition CONTEXT_PROPAGATION_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUTH-002",
                    "platform.authorization.context-propagation.default-executor requires enabled=true",
                    "Set platform.authorization.context-propagation.enabled=true or disable default-executor.",
                    500
            );

    public static final PlatformErrorDefinition CONTEXT_PROPAGATION_INVALID_SETTINGS =
            new PlatformErrorDefinition(
                    "PLT-AUTH-003",
                    "Invalid platform.authorization.context-propagation executor settings",
                    "Use core-size >= 1, max-size >= core-size, queue-capacity >= 0, nonnegative keep-alive and a nonblank thread-name-prefix.",
                    500
            );

    private AuthorizationTechnicalErrors() {
    }
}
