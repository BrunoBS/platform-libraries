package br.com.portalmanager.platform.library.parameterclient.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class ParameterClientTechnicalErrors {
    public static final PlatformErrorDefinition REDIS_REQUIRED = new PlatformErrorDefinition(
            "PLT-PARAM-005",
            "Platform Redis is required by platform-parameter-client.",
            "Enable Platform Redis or provide a RedisStore bean. Redis availability is checked when a parameter value is read.",
            500);

    private ParameterClientTechnicalErrors() {}
}