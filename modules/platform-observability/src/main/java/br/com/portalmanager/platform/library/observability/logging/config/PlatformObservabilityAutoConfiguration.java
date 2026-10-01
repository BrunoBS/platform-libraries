package br.com.portalmanager.platform.library.observability.logging.config;

import br.com.portalmanager.platform.library.observability.logging.web.PayloadErrorLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.OncePerRequestFilter;

@AutoConfiguration
@ConditionalOnClass(OncePerRequestFilter.class)
public class PlatformObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PayloadErrorLoggingFilter.class)
    public PayloadErrorLoggingFilter payloadErrorLoggingFilter() {
        return new PayloadErrorLoggingFilter();
    }
}
