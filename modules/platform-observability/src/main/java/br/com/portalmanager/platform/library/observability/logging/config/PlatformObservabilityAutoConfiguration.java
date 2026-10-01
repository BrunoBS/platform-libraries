package br.com.portalmanager.platform.library.observability.logging.config;

import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizers;
import br.com.portalmanager.platform.library.observability.logging.web.PayloadErrorLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PlatformObservabilityProperties.class)
public class PlatformObservabilityAutoConfiguration {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "org.springframework.web.filter.OncePerRequestFilter")
    @ConditionalOnMissingBean(PayloadErrorLoggingFilter.class)
    @ConditionalOnProperty(
            prefix = "platform.observability.logging.request-body",
            name = "enabled",
            havingValue = "true"
    )
    public PayloadErrorLoggingFilter payloadErrorLoggingFilter(PlatformObservabilityProperties properties) {
        long maxSize = properties.getLogging().getRequestBody().getMaxSize().toBytes();
        if (maxSize <= 0 || maxSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("platform.observability.logging.request-body.max-size must be between 1B and 2GB");
        }
        return new PayloadErrorLoggingFilter((int) maxSize);
    }

    @Bean
    public ObservabilitySecurityConfiguration observabilitySecurityConfiguration(PlatformObservabilityProperties properties) {
        return new ObservabilitySecurityConfiguration(properties);
    }

    public static final class ObservabilitySecurityConfiguration {
        public ObservabilitySecurityConfiguration(PlatformObservabilityProperties properties) {
            LogSanitizers.configureAdditionalSensitiveFields(
                    properties.getLogging().getMasking().getAdditionalSensitiveFields()
            );
        }
    }
}
