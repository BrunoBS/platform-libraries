package br.com.portalmanager.platform.library.redis.api.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

/** Builds the public configuration error without coupling API properties to Redis internals. */
final class RedisConfigurationFailure {
    private RedisConfigurationFailure() { }

    static PlatformConfigurationException invalid() {
        return new PlatformConfigurationException(
                new PlatformErrorDefinition(
                        "PLT-REDIS-008",
                        "Invalid Platform Redis configuration",
                        "Review platform.redis properties.",
                        500));
    }
}
