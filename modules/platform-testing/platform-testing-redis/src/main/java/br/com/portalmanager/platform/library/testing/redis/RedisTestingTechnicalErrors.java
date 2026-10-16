package br.com.portalmanager.platform.library.testing.redis;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class RedisTestingTechnicalErrors {
    public static PlatformErrorDefinition invalidConfiguration() {
        return new PlatformErrorDefinition(
                "PLT-TST-REDIS-001",
                "Invalid Redis test configuration",
                "Use @WithRedis with a version-pinned Redis image and an isolated test namespace.",
                500);
    }

    public static PlatformErrorDefinition startupFailed() {
        return new PlatformErrorDefinition(
                "PLT-TST-REDIS-002",
                "Redis test container could not start",
                "Check Docker availability and access to the configured Redis image.",
                500);
    }

    public static PlatformErrorDefinition cleanupFailed() {
        return new PlatformErrorDefinition(
                "PLT-TST-REDIS-003",
                "Redis test namespace cleanup failed",
                "Check the test Redis connection and namespace; cleanup never targets other"
                        + " namespaces.",
                500);
    }

    private RedisTestingTechnicalErrors() {}
}
