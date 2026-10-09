package br.com.portalmanager.platform.library.authorization.model;

/**
 * Request sent by the authorization library to the central Authorization API.
 */
public record AuthorizationRequest(
        String correlationId,
        String authorization,
        String workspaceIdentifier,
        String environmentIdentifier,
        String applicationIdentifier,
        AuthorizationAction action,
        AuthorizationLevel policy
) {
}
