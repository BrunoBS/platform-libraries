package br.com.portalmanager.platform.library.messagequeue.exception;

public class MessageConsumeException extends RuntimeException {

    public MessageConsumeException(String message, Throwable cause) {
        super(message, cause);
    }
}
