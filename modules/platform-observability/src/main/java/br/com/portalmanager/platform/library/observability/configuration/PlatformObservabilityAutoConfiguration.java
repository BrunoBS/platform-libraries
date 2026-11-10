package br.com.portalmanager.platform.library.observability.configuration;

import br.com.portalmanager.platform.library.observability.api.configuration.PlatformObservabilityProperties;
import br.com.portalmanager.platform.library.observability.api.logging.SensitiveValue;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PlatformObservabilityProperties.class)
public class PlatformObservabilityAutoConfiguration {

    @Bean
    public MaskingConfiguration sensitiveValueMaskingConfiguration(PlatformObservabilityProperties properties) {
        return new MaskingConfiguration(properties.getLogging().getMasking().isEnabled());
    }

    public static final class MaskingConfiguration {
        public MaskingConfiguration(boolean enabled) {
            SensitiveValue.setMaskingEnabled(enabled);
        }
    }

}
