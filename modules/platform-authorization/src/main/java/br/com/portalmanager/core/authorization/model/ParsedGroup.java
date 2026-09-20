package br.com.portalmanager.core.authorization.model;

public record ParsedGroup(
        String fullGroup,
        String profile,
        String environment,
        String authorizer
) {}
