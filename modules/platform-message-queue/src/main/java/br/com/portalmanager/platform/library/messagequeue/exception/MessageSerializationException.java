package br.com.portalmanager.platform.library.messagequeue.exception;

public class MessageSerializationException extends MessageQueueException {

    public MessageSerializationException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
