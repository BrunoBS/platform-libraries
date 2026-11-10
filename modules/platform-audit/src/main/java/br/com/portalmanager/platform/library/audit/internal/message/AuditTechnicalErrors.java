package br.com.portalmanager.platform.library.audit.internal.message;

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

    public static final PlatformErrorDefinition OUTBOX_REPOSITORY_MISMATCH =
            new PlatformErrorDefinition(
                    "PLT-AUD-006",
                    "The audit outbox repository does not manage the mapped outbox entity",
                    "Register an AuditOutboxRepository for the concrete entity extending AuditOutboxEntity.",
                    500
            );

    public static final PlatformErrorDefinition OBJECT_STORAGE_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-007",
                    "Platform Object Storage is required by platform-audit",
                    "Add platform-object-storage and configure the audit outbox destination.",
                    500
            );

    public static final PlatformErrorDefinition OUTBOX_DESTINATION_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-008",
                    "The audit outbox destination is missing or not configured in Object Storage",
                    "Set platform.audit.outbox.destination to a logical destination configured under platform.object-storage.destinations.",
                    500
            );

    public static final PlatformErrorDefinition OUTBOX_CONFIGURATION_INVALID =
            new PlatformErrorDefinition(
                    "PLT-AUD-009",
                    "The audit outbox retry configuration is invalid",
                    "Review the retry limits, batch size, delays, recovery window and processing lease.",
                    500
            );

    private AuditTechnicalErrors() {
    }
}
