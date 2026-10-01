package br.com.portalmanager.platform.library.authorization.model;

/**
 * Request context forwarded by the library to the central Authorization API.
 */
public record AuthorizationRequest(
        String correlationId,
        String authorization,
        String workspaceIdentifier,
        String environmentIdentifier,
        String applicationIdentifier,
        String method,
        AuthorizationLevel policy
) {
}
