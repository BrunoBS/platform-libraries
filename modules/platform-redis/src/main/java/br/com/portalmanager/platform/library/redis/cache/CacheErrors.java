package br.com.portalmanager.platform.library.redis.cache;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

/** Configuration errors use the platform definitions; no Redis exception hierarchy. */
final class CacheErrors {
    static PlatformConfigurationException types() {
        return new PlatformConfigurationException(
                new PlatformErrorDefinition(
                        "PLT-REDIS-012",
                        "Redis cache requires resolved and consistent return types per region",
                        "Declare concrete method return types and use separate cache regions for"
                                + " incompatible types.",
                        500));
    }

    static PlatformConfigurationException syncTransaction() {
        return new PlatformConfigurationException(
                new PlatformErrorDefinition(
                        "PLT-REDIS-013",
                        "Synchronized Redis cache loading is not supported inside an active"
                                + " transaction",
                        "Use sync=false for transactional methods or load outside the transaction.",
                        500));
    }

    private CacheErrors() {}
}
