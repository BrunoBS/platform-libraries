package com.empresa.platform.authorization.model;

public record ParsedGroup(
        String fullGroup,
        String profile,
        String environment,
        String authorizer
) {}
