package br.com.portalmanager.platform.library.testing.lifecycle;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformContextRunnersTest {

    @Test
    void createsIsolatedConfigurableContext() {
        PlatformContextRunners.forUserConfiguration(SampleConfiguration.class)
                .withPropertyValues("sample.value=first")
                .run(context -> assertThat(context.getBean(String.class)).isEqualTo("first"));

        PlatformContextRunners.forUserConfiguration(SampleConfiguration.class)
                .withPropertyValues("sample.value=second")
                .run(context -> assertThat(context.getBean(String.class)).isEqualTo("second"));
    }

    @Configuration(proxyBeanMethods = false)
    static class SampleConfiguration {
        @Bean
        String value(org.springframework.core.env.Environment environment) {
            return environment.getRequiredProperty("sample.value");
        }
    }
}
