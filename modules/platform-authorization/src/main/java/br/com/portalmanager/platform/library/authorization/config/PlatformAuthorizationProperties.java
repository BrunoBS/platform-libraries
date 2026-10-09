package br.com.portalmanager.platform.library.authorization.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.authorization")
public class PlatformAuthorizationProperties {

    private AuthorizationMode mode = AuthorizationMode.REAL;
    private String serviceUrl;
    private Duration connectTimeout = Duration.ofMillis(500);
    private Duration readTimeout = Duration.ofSeconds(2);
    private Retry retry = new Retry();
    private Mock mock = new Mock();
    private Headers headers = new Headers();
    private ContextPropagation contextPropagation = new ContextPropagation();

    public AuthorizationMode getMode() {
        return mode;
    }

    public void setMode(AuthorizationMode mode) {
        this.mode = mode == null ? AuthorizationMode.REAL : mode;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout == null ? Duration.ofMillis(500) : connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout == null ? Duration.ofSeconds(2) : readTimeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry == null ? new Retry() : retry;
    }

    public Headers getHeaders() { return headers; }
    public void setHeaders(Headers headers) { this.headers = headers == null ? new Headers() : headers; }

    public static class Headers {
        private String correlationId = "correlation-id";
        private String authorization = "authorization";
        private String workspaceIdentifier = "workspace-identifier";
        private String environmentIdentifier = "environment-identifier";
        private String applicationIdentifier = "application-identifier";

        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String value) { correlationId = value; }
        public String getAuthorization() { return authorization; }
        public void setAuthorization(String value) { authorization = value; }
        public String getWorkspaceIdentifier() { return workspaceIdentifier; }
        public void setWorkspaceIdentifier(String value) { workspaceIdentifier = value; }
        public String getEnvironmentIdentifier() { return environmentIdentifier; }
        public void setEnvironmentIdentifier(String value) { environmentIdentifier = value; }
        public String getApplicationIdentifier() { return applicationIdentifier; }
        public void setApplicationIdentifier(String value) { applicationIdentifier = value; }
    }

    public Mock getMock() {
        return mock;
    }

    public void setMock(Mock mock) {
        this.mock = mock == null ? new Mock() : mock;
    }

    public ContextPropagation getContextPropagation() { return contextPropagation; }
    public void setContextPropagation(ContextPropagation value) {
        this.contextPropagation = value == null ? new ContextPropagation() : value;
    }

    public static class ContextPropagation {
        private boolean enabled;
        private boolean defaultExecutor;
        private int coreSize = 4;
        private int maxSize = 16;
        private int queueCapacity = 100;
        private Duration keepAlive = Duration.ofSeconds(60);
        private String threadNamePrefix = "authorization-async-";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public boolean isDefaultExecutor() { return defaultExecutor; }
        public void setDefaultExecutor(boolean value) { defaultExecutor = value; }
        public int getCoreSize() { return coreSize; }
        public void setCoreSize(int value) { coreSize = value; }
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int value) { maxSize = value; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int value) { queueCapacity = value; }
        public Duration getKeepAlive() { return keepAlive; }
        public void setKeepAlive(Duration value) { keepAlive = value; }
        public String getThreadNamePrefix() { return threadNamePrefix; }
        public void setThreadNamePrefix(String value) { threadNamePrefix = value; }
    }

    public static class Retry {

        private int maxRetries = 2;
        private Duration initialDelay = Duration.ofMillis(200);
        private double multiplier = 2.0;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay == null ? Duration.ofMillis(200) : initialDelay;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
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
