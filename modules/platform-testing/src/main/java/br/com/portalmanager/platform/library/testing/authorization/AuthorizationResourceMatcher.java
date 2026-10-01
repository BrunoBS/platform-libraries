package br.com.portalmanager.platform.library.testing.authorization;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AuthorizationResourceMatcher {
    public static final String WORKSPACE_HEADER = "workspaceIdentifier";
    public static final String APPLICATION_HEADER = "applicationIdentifier";
    public static final String ENVIRONMENT_HEADER = "environmentIdentifier";

    private final Map<String, String> headers = new LinkedHashMap<>();

    public AuthorizationResourceMatcher workspace(String identifier) {
        return header(WORKSPACE_HEADER, identifier);
    }

    public AuthorizationResourceMatcher application(String identifier) {
        return header(APPLICATION_HEADER, identifier);
    }

    public AuthorizationResourceMatcher environment(String identifier) {
        return header(ENVIRONMENT_HEADER, identifier);
    }

    public AuthorizationResourceMatcher header(String name, String value) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Header name is required");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Header value is required");
        headers.put(name, value);
        return this;
    }

    Map<String, String> headers() {
        return Map.copyOf(headers);
    }
}
