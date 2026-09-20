package com.empresa.platform.testing.client;

public record AuthorizationRequestData(
        String token,
        String accountId,
        String environment,
        String applicationId
) {

    public static AuthorizationRequestData defaults() {
        return new AuthorizationRequestData(
                "token-integration-test",
                "account-test",
                "environment-test",
                "application-test"
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String token = "token-integration-test";
        private String accountId;
        private String environment;
        private String applicationId;

        private Builder() {
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public AuthorizationRequestData build() {
            return new AuthorizationRequestData(token, accountId, environment, applicationId);
        }
    }
}
