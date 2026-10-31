package br.com.portalmanager.platform.library.authorization.model;

import java.util.Set;

/** Explicit bootstrap allowlist. Distinct from AuthorizationPolicy's level/path metadata. */
public record AuthorizationEntryPolicy(String entryPoint, String facade, String destination, String queueUrl,
        String principal, AuthorizationAction action, AuthorizationLevel level, Set<Scope> scopes) {
    public AuthorizationEntryPolicy { scopes = scopes == null ? Set.of() : Set.copyOf(scopes); }
    public record Scope(String accountIdentifier, String applicationIdentifier, String environmentIdentifier) { }
}
