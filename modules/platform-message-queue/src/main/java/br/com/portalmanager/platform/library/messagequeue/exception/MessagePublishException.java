package br.com.portalmanager.platform.library.messagequeue.exception;

import java.util.Map;

public class MessagePublishException extends MessageQueueException {

    public MessagePublishException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public MessagePublishException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
