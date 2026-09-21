package br.com.portalmanager.platform.testing.authorization;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@TestConfiguration(proxyBeanMethods = false)
public class AuthorizationMockTestConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder authorizationRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean(name = "authorizationWireMockServer", initMethod = "start", destroyMethod = "stop")
    WireMockServer authorizationWireMockServer() {
        return new WireMockServer(wireMockConfig().dynamicPort());
    }

    @Bean
    AuthorizationMock authorizationMock(
            @Qualifier("authorizationWireMockServer") WireMockServer authorizationWireMockServer
    ) {
        return new AuthorizationMock(authorizationWireMockServer);
    }

    @Bean
    DynamicPropertyRegistrar authorizationMockProperties(
            @Qualifier("authorizationWireMockServer") WireMockServer authorizationWireMockServer
    ) {
        return registry -> {
            registry.add("platform.authorization.enabled", () -> true);
            registry.add("platform.authorization.service-url", authorizationWireMockServer::baseUrl);
            registry.add("auth.service.url", authorizationWireMockServer::baseUrl);
        };
    }
}
