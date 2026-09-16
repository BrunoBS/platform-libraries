package com.empresa.platform.audit.client;

import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public final class RestAuditEventClient implements AuditEventClient {

    private final RestClient restClient;
    private final String publishPath;

    public RestAuditEventClient(RestClient.Builder builder, PlatformAuditProperties properties) {
        this.restClient = builder.baseUrl(properties.getServiceUrl()).build();
        this.publishPath = properties.getPublishPath();
    }

    @Override
    public void publish(AuditEventRequest event) {
        restClient.post()
                .uri(publishPath)
                .contentType(MediaType.APPLICATION_JSON)
                .body(event)
                .retrieve()
                .toBodilessEntity();
    }
}
