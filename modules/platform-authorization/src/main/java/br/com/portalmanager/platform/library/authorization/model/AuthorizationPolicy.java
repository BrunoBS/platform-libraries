package br.com.portalmanager.platform.library.authorization.model;

public record AuthorizationPolicy(
        AuthorizationLevel level,
        Source source,
        String workspacePathVariable,
        String applicationPathVariable,
        String environmentPathVariable
) {

    public static final String DEFAULT_WORKSPACE_PATH_VARIABLE = "workspaceIdentifier";
    public static final String DEFAULT_APPLICATION_PATH_VARIABLE = "applicationIdentifier";
    public static final String DEFAULT_ENVIRONMENT_PATH_VARIABLE = "environmentIdentifier";

    public AuthorizationPolicy(AuthorizationLevel level, Source source) {
        this(level, source,
                DEFAULT_WORKSPACE_PATH_VARIABLE,
                DEFAULT_APPLICATION_PATH_VARIABLE,
                DEFAULT_ENVIRONMENT_PATH_VARIABLE);
    }

    public static AuthorizationPolicy open() {
        return new AuthorizationPolicy(AuthorizationLevel.OPEN, Source.DEFAULT);
    }

    public enum Source {
        METHOD,
        CLASS,
        DEFAULT
    }
}
