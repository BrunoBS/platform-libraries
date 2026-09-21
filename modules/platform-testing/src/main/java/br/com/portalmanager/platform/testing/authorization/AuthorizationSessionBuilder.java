package br.com.portalmanager.platform.testing.authorization;

import br.com.portalmanager.platform.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.authorization.model.UserSession;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class AuthorizationSessionBuilder {

    private long expirationTime = Instant.now().plusSeconds(3600).toEpochMilli();
    private String userName = "integration-test";
    private String email = "integration-test@empresa.com";
    private String accountId = "account-test";
    private String applicationId = "application-test";
    private String environmentId = "environment-test";
    private String traceId = UUID.randomUUID().toString();
    private String tokenJwt = "token-integration-test";
    private Set<String> groups = new LinkedHashSet<>(Set.of("USER"));
    private Set<ParsedGroup> authorizerGroups = new LinkedHashSet<>();

    private AuthorizationSessionBuilder() {
    }

    public static AuthorizationSessionBuilder builder() {
        return new AuthorizationSessionBuilder();
    }

    public AuthorizationSessionBuilder expirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }

    public AuthorizationSessionBuilder expired() {
        this.expirationTime = Instant.now().minusSeconds(60).toEpochMilli();
        return this;
    }

    public AuthorizationSessionBuilder userName(String userName) {
        this.userName = userName;
        return this;
    }

    public AuthorizationSessionBuilder email(String email) {
        this.email = email;
        return this;
    }

    public AuthorizationSessionBuilder accountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public AuthorizationSessionBuilder applicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public AuthorizationSessionBuilder environmentId(String environmentId) {
        this.environmentId = environmentId;
        return this;
    }

    public AuthorizationSessionBuilder traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public AuthorizationSessionBuilder tokenJwt(String tokenJwt) {
        this.tokenJwt = tokenJwt;
        return this;
    }

    public AuthorizationSessionBuilder groups(String... groups) {
        this.groups = new LinkedHashSet<>(Arrays.asList(groups));
        return this;
    }

    public AuthorizationSessionBuilder groups(Set<String> groups) {
        this.groups = groups == null ? new LinkedHashSet<>() : new LinkedHashSet<>(groups);
        return this;
    }

    public AuthorizationSessionBuilder addGroup(String group) {
        this.groups.add(group);
        return this;
    }

    public AuthorizationSessionBuilder authorizerGroups(Set<ParsedGroup> authorizerGroups) {
        this.authorizerGroups = authorizerGroups == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(authorizerGroups);
        return this;
    }

    public AuthorizationSessionBuilder addAuthorizerGroup(
            String fullGroup,
            String profile,
            String environment,
            String authorizer
    ) {
        this.authorizerGroups.add(new ParsedGroup(fullGroup, profile, environment, authorizer));
        return this;
    }

    public UserSession build() {
        UserSession session = new UserSession();
        session.setExpirationTime(expirationTime);
        session.setUserName(userName);
        session.setEmail(email);
        session.setAccountId(accountId);
        session.setApplicationId(applicationId);
        session.setEnvironmentId(environmentId);
        session.setTraceId(traceId);
        session.setTokenJwt(tokenJwt);
        session.setGroups(groups);
        session.setAuthorizerGroups(authorizerGroups);
        return session;
    }
}
