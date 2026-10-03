package br.com.portalmanager.platform.library.messagequeue.exception;

public class MessageSerializationException extends RuntimeException {

    public MessageSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
