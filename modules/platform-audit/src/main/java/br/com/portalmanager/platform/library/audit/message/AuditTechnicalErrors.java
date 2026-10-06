package br.com.portalmanager.platform.library.audit.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AuditTechnicalErrors {

    public static final PlatformErrorDefinition SERVICE_NAME_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-003",
                    "platform.audit.service-name is required",
                    "Set platform.audit.service-name to the logical name of the producing service.",
                    500
            );

    public static final PlatformErrorDefinition OUTBOX_ENTITY_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-004",
                    "A concrete JPA entity extending AuditOutboxEntity is required",
                    "Register exactly one concrete audit outbox entity in the JPA metamodel.",
                    500
            );

    public static final PlatformErrorDefinition MULTIPLE_OUTBOX_ENTITIES =
            new PlatformErrorDefinition(
                    "PLT-AUD-005",
                    "Multiple audit outbox entities are registered",
                    "Register only one concrete audit outbox entity for this persistence unit.",
                    500
            );

    private AuditTechnicalErrors() {
    }
}
