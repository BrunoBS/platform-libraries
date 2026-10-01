package br.com.portalmanager.platform.library.authorization.message;

public final class AuthorizationMessageKeys {

    public static final String CORRELATION_ID_MISSING = "authorization.correlation-id.missing";
    public static final String TOKEN_MISSING = "authorization.token.missing";
    public static final String SESSION_NOT_FOUND = "authorization.session.not.found";
    public static final String GROUPS_NOT_FOUND = "authorization.groups.not.found";
    public static final String GROUP_MISSING = "authorization.group.missing";
    public static final String OWNER_REQUIRED = "authorization.owner.required";
    public static final String RESOURCE_ACCESS_DENIED = "authorization.resource.access.denied";
    public static final String PLATFORM_ACCESS_DENIED = "authorization.platform.access.denied";
    public static final String SERVICE_UNAVAILABLE = "authorization.service.unavailable";

    private AuthorizationMessageKeys() {
    }
}
