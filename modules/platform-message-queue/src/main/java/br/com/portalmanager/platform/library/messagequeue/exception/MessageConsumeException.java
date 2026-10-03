package br.com.portalmanager.platform.library.messagequeue.exception;

import java.util.Map;

public class MessageConsumeException extends MessageQueueException {

    public MessageConsumeException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public MessageConsumeException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
