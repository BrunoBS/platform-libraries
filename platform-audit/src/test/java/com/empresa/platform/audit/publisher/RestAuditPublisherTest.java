package com.empresa.platform.audit.publisher;

import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestAuditPublisherTest {

    private final AuditEventRequest event = new AuditEventRequest(
            Instant.now(),
            "account",
            "account-1",
            "application-1",
            "dev",
            "account",
            "123",
            "UPDATE",
            "user",
            "correlation-1",
            200,
            Map.of("id", "123"),
            Map.of()
    );

    @Test
    void shouldNotPropagateHttpFailureByDefault() {
        PlatformAuditProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://audit-api/api/v1/events"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        RestAuditPublisher publisher = new RestAuditPublisher(
                builder,
                new SyncTaskExecutor(),
                properties,
                emptyFallbackProvider()
        );

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void shouldPersistEventInFallbackWhenHttpFails() {
        PlatformAuditProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://audit-api/api/v1/events"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        AuditFallbackStore fallbackStore = mock(AuditFallbackStore.class);

        RestAuditPublisher publisher = new RestAuditPublisher(
                builder,
                new SyncTaskExecutor(),
                properties,
                fallbackProvider(fallbackStore)
        );

        publisher.publish(event);

        verify(fallbackStore).save(event);
        server.verify();
    }

    @Test
    void shouldPropagateHttpFailureWhenFailOnErrorIsEnabled() {
        PlatformAuditProperties properties = properties();
        properties.setFailOnError(true);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("http://audit-api/api/v1/events"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        RestAuditPublisher publisher = new RestAuditPublisher(
                builder,
                new SyncTaskExecutor(),
                properties,
                emptyFallbackProvider()
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(RuntimeException.class);

        server.verify();
    }

    private PlatformAuditProperties properties() {
        PlatformAuditProperties properties = new PlatformAuditProperties();
        properties.setServiceUrl("http://audit-api");
        return properties;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AuditFallbackStore> emptyFallbackProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AuditFallbackStore> fallbackProvider(AuditFallbackStore fallbackStore) {
        ObjectProvider<AuditFallbackStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(fallbackStore);
        return provider;
    }
}
