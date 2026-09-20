package br.com.portalmanager.core.audit.publisher;

import br.com.portalmanager.core.audit.config.PlatformAuditProperties;
import br.com.portalmanager.core.audit.fallback.AuditFallbackStore;
import br.com.portalmanager.core.audit.model.AuditEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class RestAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(RestAuditPublisher.class);

    private final RestClient restClient;
    private final String publishPath;
    private final TaskExecutor taskExecutor;
    private final PlatformAuditProperties properties;
    private final ObjectProvider<AuditFallbackStore> fallbackStoreProvider;

    public RestAuditPublisher(
            RestClient.Builder builder,
            TaskExecutor taskExecutor,
            PlatformAuditProperties properties,
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(properties.getHttp().getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(properties.getHttp().getReadTimeout().toMillis()));

        this.restClient = builder
                .baseUrl(properties.getServiceUrl())
                .requestFactory(requestFactory)
                .build();
        this.publishPath = properties.getPublishPath();
        this.taskExecutor = taskExecutor;
        this.properties = properties;
        this.fallbackStoreProvider = fallbackStoreProvider;
    }

    @Override
    public void publish(AuditEventRequest event) {
        if (properties.isFailOnError()) {
            publishDirect(event);
            return;
        }

        try {
            taskExecutor.execute(() -> publishAsync(event));
        } catch (Exception exception) {
            log.error(
                    "Failed to schedule audit event | resource={} | resourceId={} | action={}",
                    event.resource(),
                    event.resourceId(),
                    event.action(),
                    exception
            );
            storeFallback(event);
        }
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

    private void publishAsync(AuditEventRequest event) {
        try {
            publishDirect(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to publish audit event | resource={} | resourceId={} | action={}",
                    event.resource(),
                    event.resourceId(),
                    event.action(),
                    exception
            );
            storeFallback(event);
        }
    }

    private void storeFallback(AuditEventRequest event) {
        AuditFallbackStore fallbackStore = fallbackStoreProvider.getIfAvailable();
        if (fallbackStore == null) {
            return;
        }

        try {
            fallbackStore.save(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to persist audit event in fallback store | resource={} | resourceId={} | action={}",
                    event.resource(),
                    event.resourceId(),
                    event.action(),
                    exception
            );
        }
    }
}
