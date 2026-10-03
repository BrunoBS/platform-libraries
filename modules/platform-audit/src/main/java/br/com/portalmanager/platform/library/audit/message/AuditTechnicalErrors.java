package br.com.portalmanager.platform.library.audit.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AuditTechnicalErrors {

    public static final PlatformErrorDefinition SERVICE_URL_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-001",
                    "platform.audit.service-url is required when platform.audit is enabled",
                    "Configure platform.audit.service-url with the audit service URL.",
                    500
            );

    public static final PlatformErrorDefinition FALLBACK_STORE_MISSING =
            new PlatformErrorDefinition(
                    "PLT-AUD-002",
                    "Durable audit store is required for asynchronous at-least-once delivery",
                    "Configure Redis or provide an AuditFallbackStore bean. Use fail-on-error=true only for strict synchronous delivery.",
                    500
            );

    public static final PlatformErrorDefinition REDIS_NOT_CONFIGURED =
            new PlatformErrorDefinition(
                    "PLT-AUD-003",
                    "Redis is required for the default durable audit store",
                    "Configure StringRedisTemplate, provide an AuditFallbackStore bean, or use strict synchronous delivery.",
                    500
            );

    private AuditTechnicalErrors() {
    }
}
