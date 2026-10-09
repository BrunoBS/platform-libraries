package br.com.portalmanager.platform.library.authorization.model;

/** Transport-independent authorization and optional entrypoint metadata. */
public record AuthorizationContext(
        String correlationId,
        String authorization,
        String workspaceIdentifier,
        String environmentIdentifier,
        String applicationIdentifier,
        String clientIp,
        String userAgent,
        String uri
) {
    public AuthorizationContext(String correlationId, String authorization, String workspaceIdentifier,
                                String environmentIdentifier, String applicationIdentifier) {
        this(correlationId, authorization, workspaceIdentifier, environmentIdentifier,
                applicationIdentifier, null, null, null);
    }
}
