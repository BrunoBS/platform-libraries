package br.com.portalmanager.platform.library.testing.authorization.api;

@FunctionalInterface
public interface AuthorizationSessionCustomizer {

    void customize(AuthorizationSessionBuilder session);
}
