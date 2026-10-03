package br.com.portalmanager.platform.library.audit.publisher;

import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.queue.AuditEventQueue;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class RestAuditPublisher implements AuditPublisher {

    private final RestClient restClient;
    private final String publishPath;
    private final PlatformAuditProperties properties;
    private final ObjectProvider<AuditEventQueue> eventQueueProvider;

    public RestAuditPublisher(
            RestClient.Builder builder,
            PlatformAuditProperties properties,
            ObjectProvider<AuditEventQueue> eventQueueProvider
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(properties.getHttp().getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(properties.getHttp().getReadTimeout().toMillis()));

        this.restClient = builder
                .baseUrl(properties.getServiceUrl())
                .requestFactory(requestFactory)
                .build();
        this.publishPath = properties.getPublishPath();
        this.properties = properties;
        this.eventQueueProvider = eventQueueProvider;
    }

    @Override
    public void publish(AuditEventRequest event) {
        if (properties.isFailOnError()) {
            publishDirect(event);
            return;
        }

        AuditEventQueue store = eventQueueProvider.getIfAvailable();
        if (store == null) {
            throw new IllegalStateException(
                    "Durable audit store is required for asynchronous at-least-once delivery"
            );
        }

        store.save(event);
    }

    @Override
    public void publishDirect(AuditEventRequest event) {
        restClient.post()
                .uri(publishPath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }
}
