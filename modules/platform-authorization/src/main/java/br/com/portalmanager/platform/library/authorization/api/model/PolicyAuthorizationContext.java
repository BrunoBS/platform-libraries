package br.com.portalmanager.platform.library.authorization.api.model;

/** Requested account scope; constructing this value never grants POLICY authority. */
public record PolicyAuthorizationContext(String correlationId, String accountIdentifier,
        String applicationIdentifier, String environmentIdentifier) { }
