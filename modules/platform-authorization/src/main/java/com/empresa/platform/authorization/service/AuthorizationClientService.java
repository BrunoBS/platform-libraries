package com.empresa.platform.authorization.service;

import com.empresa.platform.authorization.exception.ForbiddenAccessException;
import com.empresa.platform.authorization.exception.UnauthorizedAccessException;
import com.empresa.platform.authorization.message.AuthorizationMessageKeys;
import com.empresa.platform.authorization.model.AuthorizationLevel;
import com.empresa.platform.authorization.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
public class AuthorizationClientService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationClientService.class);

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;

    public AuthorizationClientService(RestClient.Builder builder, String authUrl) {
        this.restClient = builder.baseUrl(authUrl).build();
        this.retryTemplate = new RetryTemplate(
                RetryPolicy.builder()
                        .includes(HttpServerErrorException.class, ResourceAccessException.class)
                        .maxRetries(2)
                        .delay(Duration.ofMillis(200))
                        .multiplier(2)
                        .build()
        );
        log.info("AuthorizationClientService inicializado com sucesso na URL: {}", authUrl);
    }

    public UserSession authorize(
            String correlationId, String authorization, String account,
            String environment, String application, String method,
            AuthorizationLevel policy
    ) {
        try {
            return retryTemplate.invoke(() -> executeAuthorization(
                    correlationId,
                    authorization,
                    account,
                    environment,
                    application,
                    method,
                    policy
            ));
        }
        catch (HttpServerErrorException | ResourceAccessException exception) {
            return recover(
                    exception,
                    correlationId,
                    authorization,
                    account,
                    environment,
                    application,
                    method,
                    policy
            );
        }
    }

    private UserSession executeAuthorization(
            String correlationId, String authorization, String account,
            String environment, String application, String method,
            AuthorizationLevel policy
    ) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/authorize").build())
                .headers(headers -> {
                    setIfNotNull(headers, "X-Correlation-Id", correlationId);
                    setIfNotNull(headers, "Authorization", authorization);
                    setIfNotNull(headers, "X-Account-Id", account);
                    setIfNotNull(headers, "X-Environment", environment);
                    setIfNotNull(headers, "X-Application-Id", application);
                    setIfNotNull(headers, "X-Method", method);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    if (policy != null) {
                        headers.set("X-Policy", policy.name());
                    }
                })
                .retrieve()
                .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                    throw new HttpServerErrorException(response.getStatusCode());
                })
                .onStatus(status -> status.value() == HttpStatus.UNAUTHORIZED.value(), (request, response) -> {
                    throw new UnauthorizedAccessException(AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED);
                })
                .onStatus(status -> status.value() == HttpStatus.FORBIDDEN.value(), (request, response) -> {
                    throw new ForbiddenAccessException(AuthorizationMessageKeys.RESOURCE_ACCESS_DENIED);
                })
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    throw new HttpClientErrorException(response.getStatusCode());
                })
                .body(UserSession.class);
    }

    private void setIfNotNull(HttpHeaders headers, String headerName, Object value) {
        if (value != null) {
            headers.set(headerName, value.toString());
        }
    }

    public UserSession recover(
            Exception exception,
            String correlationId,
            String authorization,
            String account,
            String environment,
            String application,
            String method,
            AuthorizationLevel policy
    ) {
        log.error(
                "[CorrelationId: {}] Falha critica de comunicacao ou acesso negado no servidor de autenticacao.",
                correlationId,
                exception
        );
        throw new ForbiddenAccessException(AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED, exception);
    }
}
