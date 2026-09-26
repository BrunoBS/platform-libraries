package br.com.portalmanager.platform.library.testing.authorization;

@FunctionalInterface
public interface AuthorizationSessionCustomizer {

    void customize(AuthorizationSessionBuilder session);
}
