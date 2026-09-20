package br.com.portalmanager.core.messaging.model;

public record ApiValidationDetail(
        String field,
        String message
) {
}
