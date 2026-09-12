package com.empresa.platform.testing.authorization;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@TestConfiguration(proxyBeanMethods = false)
public class AuthorizationMockTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    WireMockServer authorizationWireMockServer() {
        return new WireMockServer(wireMockConfig().dynamicPort());
    }

    @Bean
    AuthorizationMock authorizationMock(WireMockServer authorizationWireMockServer) {
        return new AuthorizationMock(authorizationWireMockServer);
    }

    @Bean
    DynamicPropertyRegistrar authorizationMockProperties(WireMockServer authorizationWireMockServer) {
        return registry -> {
            registry.add("platform.authorization.enabled", () -> true);
            registry.add("platform.authorization.service-url", authorizationWireMockServer::baseUrl);
            registry.add("auth.service.url", authorizationWireMockServer::baseUrl);
        };
    }
}
