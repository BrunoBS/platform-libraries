package br.com.portalmanager.platform.library.audit.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AuditTechnicalErrors {

    public static final PlatformErrorDefinition MESSAGE_QUEUE_DESTINATION_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-001",
                    "platform.audit.destination is required",
                    "Configure platform.audit.destination with a logical platform-message-queue destination.",
                    500
            );

    public static final PlatformErrorDefinition MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED =
            new PlatformErrorDefinition(
                    "PLT-AUD-002",
                    "The audit destination is not configured for ordered publishing",
                    "Configure the audit destination in platform.message-queue.destinations with ordered=true and publisher.enabled=true.",
                    500
            );

    public static final PlatformErrorDefinition SERVICE_NAME_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-003",
                    "platform.audit.service-name is required",
                    "Set platform.audit.service-name to the logical name of the producing service.",
                    500
            );

    public static final PlatformErrorDefinition EVENT_SIZE_LIMIT_INVALID =
            new PlatformErrorDefinition(
                    "PLT-AUD-004",
                    "platform.audit.max-event-size-bytes must be positive",
                    "Set platform.audit.max-event-size-bytes to a positive number.",
                    500
            );

    public static final PlatformErrorDefinition EVENT_COUNT_LIMIT_INVALID =
            new PlatformErrorDefinition(
                    "PLT-AUD-005",
                    "platform.audit.max-events-per-invocation must be positive",
                    "Set platform.audit.max-events-per-invocation to a positive number.",
                    500
            );

    private AuditTechnicalErrors() {
    }
}
