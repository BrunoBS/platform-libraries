package br.com.portalmanager.platform.library.authorization.api.model;

public record ParsedGroup(
        String fullGroup,
        String profile,
        String environment,
        String authorizer
) {}
