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
                    "Audit fallback is enabled without a configured fallback store",
                    "Configure Redis or provide an AuditFallbackStore bean.",
                    500
            );

    public static final PlatformErrorDefinition REDIS_NOT_CONFIGURED =
            new PlatformErrorDefinition(
                    "PLT-AUD-003",
                    "Redis is required when platform.audit.fallback is enabled",
                    "Configure StringRedisTemplate or disable platform.audit.fallback.enabled.",
                    500
            );

    private AuditTechnicalErrors() {
    }
}
