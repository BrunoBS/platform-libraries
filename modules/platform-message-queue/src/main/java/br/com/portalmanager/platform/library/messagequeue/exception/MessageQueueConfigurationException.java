package br.com.portalmanager.platform.library.messagequeue.exception;

public class MessageQueueConfigurationException extends RuntimeException {

    public MessageQueueConfigurationException(String message) {
        super(message);
    }

    public MessageQueueConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
