package com.empresa.platform.audit.client;

import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public final class RestAuditEventClient implements AuditEventClient {

    private final RestClient restClient;
    private final String publishPath;

    public RestAuditEventClient(RestClient.Builder builder, PlatformAuditProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(properties.getHttp().getConnectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(properties.getHttp().getReadTimeout().toMillis()));

        this.restClient = builder
                .baseUrl(properties.getServiceUrl())
                .requestFactory(requestFactory)
                .build();
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
