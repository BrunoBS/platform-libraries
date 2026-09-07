package com.empresa.platform.authorization.service;

import com.empresa.platform.authorization.model.AuthorizationLevel;
import com.empresa.platform.authorization.model.UserSession;
import com.empresa.platform.messaging.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class AuthorizationClientService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationClientService.class);

    private final RestClient restClient;

    public AuthorizationClientService(
            RestClient.Builder builder,
            @Value("${platform.authorization.service-url}") String authUrl
    ) {
        this.restClient = builder.baseUrl(authUrl).build();
        log.info("AuthorizationClientService inicializado com sucesso na URL: {}", authUrl);
    }

    @Retryable(
            retryFor = {HttpServerErrorException.class, ResourceAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2)
    )
    public UserSession authorize(
            String correlationId, String authorization, String account,
            String environment, String application, String method,
            AuthorizationLevel policy
    ) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/authorize").build())
                .headers(h -> {
                    setIfNotNull(h, "X-Correlation-Id", correlationId);
                    setIfNotNull(h, "Authorization", authorization);
                    setIfNotNull(h, "X-Account-Id", account);
                    setIfNotNull(h, "X-Environment", environment);
                    setIfNotNull(h, "X-Application-Id", application);
                    setIfNotNull(h, "X-Method", method);
                    h.setContentType(MediaType.APPLICATION_JSON);
                    if (policy != null) h.set("X-Policy", policy.name());
                })
                .retrieve()
                .onStatus(s -> s.is5xxServerError(), (req, res) -> {
                    throw new HttpServerErrorException(res.getStatusCode());
                })
                .onStatus(s -> s.is4xxClientError(), (req, res) -> {
                    throw new HttpClientErrorException(res.getStatusCode());
                })
                .body(UserSession.class);
    }

    private void setIfNotNull(HttpHeaders headers, String headerName, Object value) {
        if (value != null) {
            headers.set(headerName, value.toString());
        }
    }

    @Recover
    public UserSession recover(Exception e, String correlationId, String authorization,
                               String account, String environment, String application,
                               String method, AuthorizationLevel policy) {

        log.error("[CorrelationId: {}] Falha critica de comunicacao ou acesso negado no servidor de autenticacao.", correlationId, e);

        // Lança a exceção da platform-messaging que acionará o catálogo em português automaticamente
        throw new ForbiddenException("PLATFORM_ACCESS_DENIED", e);
    }
}
