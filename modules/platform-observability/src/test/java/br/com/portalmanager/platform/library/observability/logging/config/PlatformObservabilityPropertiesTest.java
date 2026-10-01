package br.com.portalmanager.platform.library.observability.logging.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformObservabilityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformObservabilityAutoConfiguration.class));

    @Test
    void shouldUseSecureDefaults() {
        contextRunner.run(context -> {
            PlatformObservabilityProperties properties = context.getBean(PlatformObservabilityProperties.class);

            assertThat(properties.getLogging().getMasking().isEnabled()).isTrue();
            assertThat(properties.getLogging().getRequestBody().isEnabled()).isFalse();
        });
    }

    @Test
    void shouldBindLoggingConfigurationFromSingleRoot() {
        contextRunner.withPropertyValues(
                "platform.observability.logging.masking.enabled=false",
                "platform.observability.logging.request-body.enabled=true",
                "platform.observability.logging.levels.br.com.portalmanager=DEBUG",
                "platform.observability.logging.defaults.org.springframework=ERROR",
                "platform.observability.logging.custom-converters.audit=com.example.AuditConverter"
        ).run(context -> {
            PlatformObservabilityProperties properties = context.getBean(PlatformObservabilityProperties.class);

            assertThat(properties.getLogging().getMasking().isEnabled()).isFalse();
            assertThat(properties.getLogging().getRequestBody().isEnabled()).isTrue();
            assertThat(properties.getLogging().getLevels()).containsEntry("br.com.portalmanager", "DEBUG");
            assertThat(properties.getLogging().getDefaults()).containsEntry("org.springframework", "ERROR");
            assertThat(properties.getLogging().getCustomConverters()).containsEntry("audit", "com.example.AuditConverter");
        });
    }
}
