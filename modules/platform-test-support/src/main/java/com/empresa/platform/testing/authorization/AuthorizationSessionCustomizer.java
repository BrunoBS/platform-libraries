package com.empresa.platform.testing.authorization;

@FunctionalInterface
public interface AuthorizationSessionCustomizer {

    void customize(AuthorizationSessionBuilder session);
}
