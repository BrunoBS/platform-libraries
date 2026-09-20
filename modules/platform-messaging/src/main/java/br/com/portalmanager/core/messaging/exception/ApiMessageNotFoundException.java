package br.com.portalmanager.core.messaging.exception;

public class ApiMessageNotFoundException extends RuntimeException {

    private final String messageKey;

    public ApiMessageNotFoundException(String messageKey) {
        super("Mensagem de API nao encontrada para a chave: " + messageKey);
        this.messageKey = messageKey;
    }

    public ApiMessageNotFoundException(String messageKey, Throwable cause) {
        super("Mensagem de API nao encontrada para a chave: " + messageKey, cause);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
