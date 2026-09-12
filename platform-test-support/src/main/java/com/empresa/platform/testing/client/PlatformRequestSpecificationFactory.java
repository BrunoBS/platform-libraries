package com.empresa.platform.testing.client;

import com.empresa.platform.testing.context.TestContext;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.core.env.Environment;

import java.util.List;

public final class PlatformRequestSpecificationFactory {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String ACCOUNT_ID_HEADER = "X-Account-Id";
    public static final String ENVIRONMENT_HEADER = "X-Environment";
    public static final String APPLICATION_ID_HEADER = "X-Application-Id";

    private final Environment environment;
    private final List<PlatformRequestSpecificationCustomizer> customizers;

    public PlatformRequestSpecificationFactory(
            Environment environment,
            List<PlatformRequestSpecificationCustomizer> customizers
    ) {
        this.environment = environment;
        this.customizers = List.copyOf(customizers);
    }

    public RequestSpecification create() {
        return build(null);
    }

    public RequestSpecification createAuthorized() {
        return build(AuthorizationRequestData.defaults());
    }

    public RequestSpecification createAuthorized(AuthorizationRequestData authorization) {
        if (authorization == null) {
            throw new IllegalArgumentException("Authorization request data must not be null");
        }
        return build(authorization);
    }

    private RequestSpecification build(AuthorizationRequestData authorization) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setPort(environment.getRequiredProperty("local.server.port", Integer.class))
                .setContentType(ContentType.JSON)
                .addHeader(CORRELATION_ID_HEADER, TestContext.correlationId());

        if (authorization != null) {
            addIfPresent(builder, AUTHORIZATION_HEADER, bearer(authorization.token()));
            addIfPresent(builder, ACCOUNT_ID_HEADER, authorization.accountId());
            addIfPresent(builder, ENVIRONMENT_HEADER, authorization.environment());
            addIfPresent(builder, APPLICATION_ID_HEADER, authorization.applicationId());
        }

        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    private static String bearer(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return token.regionMatches(true, 0, "Bearer ", 0, 7)
                ? token
                : "Bearer " + token;
    }

    private static void addIfPresent(RequestSpecBuilder builder, String header, String value) {
        if (value != null && !value.isBlank()) {
            builder.addHeader(header, value);
        }
    }
}
