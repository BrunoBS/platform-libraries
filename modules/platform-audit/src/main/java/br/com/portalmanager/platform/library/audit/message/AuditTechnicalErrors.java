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

    private AuditTechnicalErrors() {
    }
}
