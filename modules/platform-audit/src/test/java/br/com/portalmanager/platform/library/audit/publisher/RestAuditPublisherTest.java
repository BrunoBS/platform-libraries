package br.com.portalmanager.platform.library.audit.publisher;

import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.queue.AuditEventQueue;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RestAuditPublisherTest {

    private final AuditEventRequest event = new AuditEventRequest(
            Instant.now(), "account", "account-1", "application-1", "dev",
            "account", "123", "UPDATE", "user", "correlation-1", 200,
            Map.of("id", "123"), Map.of()
    );

    @Test
    void shouldPersistBeforeAsynchronousDelivery() {
        PlatformAuditProperties properties = properties();
        AuditEventQueue store = mock(AuditEventQueue.class);

        RestAuditPublisher publisher = new RestAuditPublisher(
                failingBuilder(),
                properties,
                queueProvider(store)
        );

        publisher.publish(event);

        verify(store).save(event);
    }

    @Test
    void shouldNotCallAuditApiFromBusinessThreadInDurableMode() {
        PlatformAuditProperties properties = properties();
        AuditEventQueue store = mock(AuditEventQueue.class);
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        RestAuditPublisher publisher = new RestAuditPublisher(builder, properties, queueProvider(store));

        publisher.publish(event);

        verify(store).save(event);
        verifyNoInteractions(restClient);
    }

    @Test
    void shouldPropagateQueueFailure() {
        PlatformAuditProperties properties = properties();
        AuditEventQueue store = mock(AuditEventQueue.class);
        whenStoreSaveFails(store);

        RestAuditPublisher publisher = new RestAuditPublisher(
                failingBuilder(),
                properties,
                queueProvider(store)
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("redis unavailable");
    }

    @Test
    void shouldPropagateHttpFailureWhenFailOnErrorIsEnabled() {
        PlatformAuditProperties properties = properties();
        properties.setFailOnError(true);

        RestAuditPublisher publisher = new RestAuditPublisher(
                failingBuilder(),
                properties,
                emptyQueueProvider()
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("audit unavailable");
    }

    private void whenStoreSaveFails(AuditEventQueue store) {
        org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
                .when(store).save(event);
    }

    private RestClient.Builder failingBuilder() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.post().uri(anyString()).contentType(MediaType.APPLICATION_JSON)
                .body(any(Object.class)).retrieve().toBodilessEntity())
                .thenThrow(new IllegalStateException("audit unavailable"));
        return builder;
    }

    private PlatformAuditProperties properties() {
        PlatformAuditProperties properties = new PlatformAuditProperties();
        properties.setServiceUrl("http://audit-api");
        return properties;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AuditEventQueue> emptyQueueProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<AuditEventQueue> queueProvider(AuditEventQueue store) {
        ObjectProvider<AuditEventQueue> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        return provider;
    }
}
