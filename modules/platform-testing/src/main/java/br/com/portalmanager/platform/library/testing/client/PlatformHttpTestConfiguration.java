package br.com.portalmanager.platform.library.testing.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@TestConfiguration(proxyBeanMethods = false)
public class PlatformHttpTestConfiguration {

    @Bean
    PlatformRequestSpecificationFactory platformRequestSpecificationFactory(
            Environment environment,
            ObjectProvider<PlatformRequestSpecificationCustomizer> customizers
    ) {
        return new PlatformRequestSpecificationFactory(
                environment,
                customizers.orderedStream().toList()
        );
    }
}
