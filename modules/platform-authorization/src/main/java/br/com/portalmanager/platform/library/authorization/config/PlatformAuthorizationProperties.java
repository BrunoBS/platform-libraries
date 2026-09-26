package br.com.portalmanager.platform.library.authorization.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.authorization")
public class PlatformAuthorizationProperties {

    private boolean enabled = true;
    private String serviceUrl;
    private Mock mock = new Mock();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public Mock getMock() {
        return mock;
    }

    public void setMock(Mock mock) {
        this.mock = mock == null ? new Mock() : mock;
    }

    public static class Mock {

        private String userName = "guest";
        private String email = "guest@empresa.com";
        private String accountId = "account-guest";
        private String applicationId = "application-guest";
        private String environmentId = "environment-guest";
        private String traceId = "trace-guest";
        private Set<String> groups = new LinkedHashSet<>(Set.of("GUEST"));
        private List<AuthorizerGroup> authorizerGroups = List.of();

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String getEnvironmentId() {
            return environmentId;
        }

        public void setEnvironmentId(String environmentId) {
            this.environmentId = environmentId;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public Set<String> getGroups() {
            return groups;
        }

        public void setGroups(Set<String> groups) {
            this.groups = groups == null || groups.isEmpty()
                    ? new LinkedHashSet<>(Set.of("GUEST"))
                    : new LinkedHashSet<>(groups);
        }

        public List<AuthorizerGroup> getAuthorizerGroups() {
            return authorizerGroups;
        }

        public void setAuthorizerGroups(List<AuthorizerGroup> authorizerGroups) {
            this.authorizerGroups = authorizerGroups == null ? List.of() : List.copyOf(authorizerGroups);
        }
    }

    public static class AuthorizerGroup {

        private String fullGroup;
        private String profile;
        private String environment;
        private String authorizer;

        public AuthorizerGroup() {
        }

        public AuthorizerGroup(
                String fullGroup,
                String profile,
                String environment,
                String authorizer
        ) {
            this.fullGroup = fullGroup;
            this.profile = profile;
            this.environment = environment;
            this.authorizer = authorizer;
        }

        public String getFullGroup() {
            return fullGroup;
        }

        public void setFullGroup(String fullGroup) {
            this.fullGroup = fullGroup;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public String getAuthorizer() {
            return authorizer;
        }

        public void setAuthorizer(String authorizer) {
            this.authorizer = authorizer;
        }
    }
}
