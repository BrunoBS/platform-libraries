package br.com.portalmanager.platform.authorization.model;

public record AuthorizationPolicy(
        AuthorizationLevel level,
        Source source
) {

    public static AuthorizationPolicy open() {
        return new AuthorizationPolicy(
                AuthorizationLevel.OPEN,
                Source.DEFAULT
        );
    }

    public enum Source {
        METHOD,
        CLASS_POLICY,
        CLASS,
        DEFAULT
    }
}
