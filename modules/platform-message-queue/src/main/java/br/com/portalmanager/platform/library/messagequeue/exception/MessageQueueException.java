package br.com.portalmanager.platform.library.messagequeue.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;

import java.util.Map;

public abstract class MessageQueueException extends ApiException {

    protected MessageQueueException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    protected MessageQueueException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
