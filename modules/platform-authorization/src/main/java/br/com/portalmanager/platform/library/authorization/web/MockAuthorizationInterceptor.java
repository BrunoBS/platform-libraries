package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.model.AuthorizerGroupParser;
import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Set;

/**
 * Creates the local authorization session used exclusively in MOCK mode.
 */
public class MockAuthorizationInterceptor implements HandlerInterceptor {

    private final PlatformAuthorizationProperties properties;

    public MockAuthorizationInterceptor(PlatformAuthorizationProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler
    ) {
        UserSession session = createSession(properties.getMock());
        AuthorizationRequestContext.set(session, request, "mock-agent");
        return true;
    }

    private UserSession createSession(PlatformAuthorizationProperties.Mock mock) {
        UserSession session = new UserSession();
        session.setUserName(mock.getUserName());
        session.setEmail(mock.getEmail());
        session.setAccountId(mock.getAccountId());
        session.setApplicationId(mock.getApplicationId());
        session.setEnvironmentId(mock.getEnvironmentId());
        session.setTraceId(mock.getTraceId());
        session.setExpirationTime(Instant.now().plusSeconds(3600).toEpochMilli());
        session.setGroups(mock.getGroups());
        session.setAuthorizerGroups(resolveAuthorizerGroups(mock));
        return session;
    }

    private Set<ParsedGroup> resolveAuthorizerGroups(PlatformAuthorizationProperties.Mock mock) {
        if (!mock.getAuthorizerGroups().isEmpty()) {
            return Set.copyOf(mock.getAuthorizerGroups().stream()
                    .map(group -> new ParsedGroup(
                            group.getFullGroup(),
                            group.getProfile(),
                            group.getEnvironment(),
                            group.getAuthorizer()))
                    .toList());
        }

        Set<ParsedGroup> parsedGroups = AuthorizerGroupParser.parseAll(mock.getGroups());
        if (!parsedGroups.isEmpty()) {
            return parsedGroups;
        }

        return Set.of(new ParsedGroup("GUEST", "GUEST", mock.getEnvironmentId(), "GUEST"));
    }

    @Override
    public void afterCompletion(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler,
            Exception ex
    ) {
        AuthorizationRequestContext.clear();
    }
}
