package br.com.portalmanager.platform.messaging.model;

public record ApiValidationDetail(
        String field,
        String message
) {
}
