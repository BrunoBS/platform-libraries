package br.com.portalmanager.platform.library.messaging.model;

public record ApiMessage(
        String code,
        String messageKey,
        String locale,
        String message,
        String solution,
        int httpStatus
) {
    public ApiMessage {
        code = requireText(code, "code");
        messageKey = requireText(messageKey, "messageKey");
        locale = requireText(locale, "locale");
        message = requireText(message, "message");

        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("ApiMessage.httpStatus must be between 100 and 599");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ApiMessage." + field + " must not be blank");
        }
        return value;
    }
}
