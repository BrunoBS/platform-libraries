package br.com.portalmanager.platform.library.authorization.model;

public record ParsedGroup(
        String fullGroup,
        String profile,
        String environment,
        String authorizer
) {}
