package br.com.portalmanager.platform.testing.authorization;

@FunctionalInterface
public interface AuthorizationSessionCustomizer {

    void customize(AuthorizationSessionBuilder session);
}
