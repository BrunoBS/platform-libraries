package br.com.portalmanager.platform.library.messaging.model;

public record PlatformErrorDefinition(
        String code,
        String message,
        String solution,
        int httpStatus
) {
}
