package br.com.portalmanager.platform.library.messagequeue.exception;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueTechnicalErrors;

public class MessageQueueConfigurationException extends PlatformConfigurationException {

    public MessageQueueConfigurationException(String reason) {
        this(reason, null);
    }

    public MessageQueueConfigurationException(String reason, Throwable cause) {
        super(MessageQueueTechnicalErrors.invalidConfiguration(reason), cause);
    }
}
