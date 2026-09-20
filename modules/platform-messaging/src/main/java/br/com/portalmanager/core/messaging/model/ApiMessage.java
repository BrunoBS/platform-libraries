package br.com.portalmanager.core.messaging.model;

public record ApiMessage(
        String code,
        String messageKey,
        String locale,
        String message,
        String solution,
        int httpStatus) {
}
