package br.com.portalmanager.platform.messaging.model;

public record ApiMessage(
        String code,
        String messageKey,
        String locale,
        String message,
        String solution,
        int httpStatus) {
}
