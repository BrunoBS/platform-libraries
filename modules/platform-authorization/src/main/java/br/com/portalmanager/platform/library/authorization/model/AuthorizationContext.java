package br.com.portalmanager.platform.library.authorization.model;

/**
 * Transport-independent authorization data supplied by an application entrypoint.
 */
public record AuthorizationContext(
        String correlationId,
        String authorization,
        String workspaceIdentifier,
        String environmentIdentifier,
        String applicationIdentifier
) {
}
