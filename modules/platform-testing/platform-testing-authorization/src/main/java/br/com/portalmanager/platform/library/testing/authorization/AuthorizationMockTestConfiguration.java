package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.testing.http.HttpMockServer;
import br.com.portalmanager.platform.library.testing.http.HttpMockTestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.web.client.RestClient;

@TestConfiguration(proxyBeanMethods = false)
@Import(HttpMockTestConfiguration.class)
public class AuthorizationMockTestConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder authorizationRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    AuthorizationMock authorizationMock(HttpMockServer httpMockServer) {
        return new AuthorizationMock(httpMockServer);
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationClient.class)
    AuthorizationClient authorizationMockClient(
            RestClient.Builder builder,
            AuthorizationMock authorizationMock,
            PlatformAuthorizationProperties properties
    ) {
        return new AuthorizationClient(builder, authorizationMock.baseUrl(), properties.getRetry());
    }

    @Bean
    DynamicPropertyRegistrar authorizationMockProperties(HttpMockServer httpMockServer) {
        return registry -> {
            registry.add("platform.authorization.enabled", () -> true);
            registry.add("platform.authorization.service-url", httpMockServer::baseUrl);
            registry.add("auth.service.url", httpMockServer::baseUrl);
        };
    }
}
