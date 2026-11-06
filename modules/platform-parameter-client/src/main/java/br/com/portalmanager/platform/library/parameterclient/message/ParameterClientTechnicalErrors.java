package br.com.portalmanager.platform.library.parameterclient.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class ParameterClientTechnicalErrors {
    public static final PlatformErrorDefinition REDIS_REQUIRED = new PlatformErrorDefinition(
            "PLT-PARAM-005",
            "Redis is required by platform-parameter-client and is unavailable during application startup.",
            "Configure Redis through Platform Redis and make it reachable before starting the service.",
            500);

    private ParameterClientTechnicalErrors() {}
}
