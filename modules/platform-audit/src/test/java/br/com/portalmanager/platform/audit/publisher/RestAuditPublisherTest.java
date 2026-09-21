package br.com.portalmanager.platform.audit.publisher;

import br.com.portalmanager.platform.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.audit.fallback.AuditFallbackStore;
import br.com.portalmanager.platform.audit.model.AuditEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        RestClient.Builder builder = failingBuilder();

        RestAuditPublisher publisher = new RestAuditPublisher(
                builder,
                new SyncTaskExecutor(),
                properties,
                emptyFallbackProvider()
        );

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void shouldPersistEventInFallbackWhenHttpFails() {
        PlatformAuditProperties properties = properties();
        AuditFallbackStore fallbackStore = mock(AuditFallbackStore.class);

        RestAuditPublisher publisher = new RestAuditPublisher(
                failingBuilder(),
                new SyncTaskExecutor(),
                properties,
                fallbackProvider(fallbackStore)
        );

        publisher.publish(event);

        verify(fallbackStore).save(event);
    }

    @Test
    void shouldPropagateHttpFailureWhenFailOnErrorIsEnabled() {
        PlatformAuditProperties properties = properties();
        properties.setFailOnError(true);

        RestAuditPublisher publisher = new RestAuditPublisher(
                failingBuilder(),
                new SyncTaskExecutor(),
                properties,
                emptyFallbackProvider()
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("audit unavailable");
    }

    private RestClient.Builder failingBuilder() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        when(restClient.post()
                .uri(anyString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(any(Object.class))
                .retrieve()
                .toBodilessEntity())
                .thenThrow(new IllegalStateException("audit unavailable"));

        return builder;
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
