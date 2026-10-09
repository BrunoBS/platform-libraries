package br.com.portalmanager.platform.library.authorization.client;

import br.com.portalmanager.platform.library.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.exception.AuthorizationServiceUnavailableException;
import br.com.portalmanager.platform.library.authorization.exception.AuthorizationContractException;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequest;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;


public class AuthorizationClient {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationClient.class);

    private final RestClient restClient;
    private final RetryTemplate retryTemplate;

    public AuthorizationClient(
            RestClient.Builder builder,
            String authUrl,
            PlatformAuthorizationProperties.Retry retry
    ) {
        this.restClient = builder.baseUrl(authUrl).build();
        this.retryTemplate = new RetryTemplate(
                RetryPolicy.builder()
                        .includes(HttpServerErrorException.class, ResourceAccessException.class)
                        .maxRetries(retry.getMaxRetries())
                        .delay(retry.getInitialDelay())
                        .multiplier(retry.getMultiplier())
                        .build()
        );
        log.info("AuthorizationClient inicializado com sucesso na URL: {}", authUrl);
    }

    public UserSession authorize(AuthorizationRequest request) {
        try {
            return retryTemplate.invoke(() -> executeAuthorization(request));
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            return recover(exception, request);
        }
    }

    private UserSession executeAuthorization(AuthorizationRequest request) {
        return restClient.post()
                .uri(uriBuilder -> uriBuilder.path("/authorize").build())
                .headers(headers -> {
                    setIfNotNull(headers, "correlationId", request.correlationId());
                    setIfNotNull(headers, "Authorization", request.authorization());
                    setIfNotNull(headers, "workspaceIdentifier", request.workspaceIdentifier());
                    setIfNotNull(headers, "environmentIdentifier", request.environmentIdentifier());
                    setIfNotNull(headers, "applicationIdentifier", request.applicationIdentifier());
                    setIfNotNull(headers, "action", request.action() == null ? null : request.action().name());
                    if (request.policy() != null) {
                        headers.set("policy", request.policy().name());
                    }
                })
                .retrieve()
                .onStatus(status -> status.is5xxServerError(), (httpRequest, response) -> {
                    throw new HttpServerErrorException(response.getStatusCode());
                })
                .onStatus(status -> status.value() == HttpStatus.UNAUTHORIZED.value(), (httpRequest, response) -> {
                    throw new UnauthorizedAccessException(AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED);
                })
                .onStatus(status -> status.value() == HttpStatus.FORBIDDEN.value(), (httpRequest, response) -> {
                    throw new ForbiddenAccessException(AuthorizationMessageKeys.RESOURCE_ACCESS_DENIED);
                })
                .onStatus(status -> status.is4xxClientError(), (httpRequest, response) -> {
                    throw new AuthorizationContractException(new HttpClientErrorException(response.getStatusCode()));
                })
                .body(UserSession.class);
    }

    private void setIfNotNull(HttpHeaders headers, String headerName, Object value) {
        if (value != null) {
            headers.set(headerName, value.toString());
        }
    }

    UserSession recover(Exception exception, AuthorizationRequest request) {
        log.error(
                "[CorrelationId: {}] Falha critica de comunicacao com a Authorization API.",
                request.correlationId(),
                exception
        );
        throw new AuthorizationServiceUnavailableException(exception);
    }
}
