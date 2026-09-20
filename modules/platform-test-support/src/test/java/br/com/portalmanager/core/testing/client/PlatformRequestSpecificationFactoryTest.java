package br.com.portalmanager.core.testing.client;

import br.com.portalmanager.core.testing.context.TestContext;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformRequestSpecificationFactoryTest {

    private final MockEnvironment environment = new MockEnvironment()
            .withProperty("local.server.port", "8181");

    @AfterEach
    void cleanUp() {
        TestContext.reset();
    }

    @Test
    void shouldCreateRequestWithoutAuthorizationByDefault() {
        RequestSpecification request = factory().create();
        QueryableRequestSpecification specification = SpecificationQuerier.query(request);

        assertThat(specification.getPort()).isEqualTo(8181);
        assertThat(specification.getHeaders().getValue(PlatformRequestSpecificationFactory.CORRELATION_ID_HEADER))
                .isEqualTo(TestContext.correlationId());
        assertThat(specification.getHeaders().getValue(
                PlatformRequestSpecificationFactory.AUTHORIZATION_HEADER
        )).isNull();
    }

    @Test
    void shouldAddPlatformAuthorizationHeadersOnlyWhenRequested() {
        AuthorizationRequestData authorization = AuthorizationRequestData.builder()
                .token("custom-token")
                .accountId("account-123")
                .environment("DEV")
                .applicationId("application-456")
                .build();

        QueryableRequestSpecification specification = SpecificationQuerier.query(
                factory().createAuthorized(authorization)
        );

        assertThat(specification.getHeaders().getValue("Authorization")).isEqualTo("Bearer custom-token");
        assertThat(specification.getHeaders().getValue("X-Account-Id")).isEqualTo("account-123");
        assertThat(specification.getHeaders().getValue("X-Environment")).isEqualTo("DEV");
        assertThat(specification.getHeaders().getValue("X-Application-Id")).isEqualTo("application-456");
    }

    @Test
    void shouldApplyRequestCustomizersByComposition() {
        PlatformRequestSpecificationFactory factory = new PlatformRequestSpecificationFactory(
                environment,
                List.of(builder -> builder.addHeader("X-Test-Header", "custom-value"))
        );

        QueryableRequestSpecification specification = SpecificationQuerier.query(factory.create());

        assertThat(specification.getHeaders().getValue("X-Test-Header")).isEqualTo("custom-value");
    }

    private PlatformRequestSpecificationFactory factory() {
        return new PlatformRequestSpecificationFactory(environment, List.of());
    }
}
