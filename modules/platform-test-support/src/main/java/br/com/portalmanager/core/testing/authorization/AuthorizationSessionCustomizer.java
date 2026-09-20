package br.com.portalmanager.core.testing.authorization;

@FunctionalInterface
public interface AuthorizationSessionCustomizer {

    void customize(AuthorizationSessionBuilder session);
}
