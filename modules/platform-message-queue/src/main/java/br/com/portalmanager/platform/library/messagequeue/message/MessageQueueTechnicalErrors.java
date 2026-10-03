package br.com.portalmanager.platform.library.messagequeue.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class MessageQueueTechnicalErrors {

    public static PlatformErrorDefinition invalidConfiguration(String reason) {
        return new PlatformErrorDefinition(
                "PLT-MQ-001",
                "Invalid platform.message-queue configuration: " + reason,
                "Review the message-queue provider, destination and listener settings.",
                500
        );
    }

    private MessageQueueTechnicalErrors() {
    }
}
