package br.com.portalmanager.platform.library.messaging.api.model;

public record PlatformErrorDefinition(
        String code,
        String message,
        String solution,
        int httpStatus
) {
}
