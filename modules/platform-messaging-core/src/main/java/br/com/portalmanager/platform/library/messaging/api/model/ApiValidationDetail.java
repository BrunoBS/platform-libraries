package br.com.portalmanager.platform.library.messaging.api.model;

public record ApiValidationDetail(
        String field,
        String code,
        String message,
        String solution
) {
    public ApiValidationDetail(String field, String message) {
        this(field, null, message, null);
    }
}
