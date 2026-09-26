package br.com.portalmanager.platform.library.messaging.autoconfigure;

import br.com.portalmanager.platform.library.messaging.cache.ApiMessageCache;
import br.com.portalmanager.platform.library.messaging.cache.NoOpApiMessageCache;
import br.com.portalmanager.platform.library.messaging.config.PlatformMessagingProperties;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.library.messaging.provider.ApiMessageProvider;
import br.com.portalmanager.platform.library.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.repository.NoOpApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import br.com.portalmanager.platform.library.messaging.resolver.DefaultApiMessageResolver;
import br.com.portalmanager.platform.library.messaging.web.ApiExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Locale;

@AutoConfiguration
@EnableConfigurationProperties(PlatformMessagingProperties.class)
@ConditionalOnProperty(prefix = "platform.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ApiMessageRepository.class)
    ApiMessageRepository noOpApiMessageRepository() {
        return new NoOpApiMessageRepository();
    }

    @Bean
    @ConditionalOnMissingBean(ApiMessageCache.class)
    @ConditionalOnProperty(prefix = "platform.messaging.cache", name = "enabled", havingValue = "false", matchIfMissing = true)
    ApiMessageCache noOpApiMessageCache() {
        return new NoOpApiMessageCache();
    }

    @Bean
    @ConditionalOnMissingBean(ApiMessageProvider.class)
    ApiMessageProvider apiMessageProvider(Environment environment) {
        return new PlatformDefaultMessageProvider(requireApplicationName(environment));
    }

    @Bean
    @ConditionalOnMissingBean
    ApiMessageResolver apiMessageResolver(
            ApiMessageRepository repository,
            ApiMessageCache cache,
            PlatformMessagingProperties properties,
            ApiMessageProvider provider,
            Environment environment
    ) {
        Locale yamlLocale = Locale.forLanguageTag(properties.getDefaultLocale());
        Locale safeDefault = yamlLocale.equals(Locale.ROOT)
                ? Locale.forLanguageTag("pt-BR")
                : yamlLocale;
        return new DefaultApiMessageResolver(
                repository,
                cache,
                safeDefault,
                provider,
                requireApplicationName(environment)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    ApiExceptionHandler apiExceptionHandler(ApiMessageResolver resolver) {
        return new ApiExceptionHandler(resolver);
    }

    private String requireApplicationName(Environment environment) {
        String applicationName = environment.getProperty("spring.application.name");
        if (applicationName == null || applicationName.isBlank()) {
            throw new PlatformConfigurationException(
                    PlatformTechnicalErrors.APPLICATION_NAME_REQUIRED
            );
        }
        return applicationName.trim();
    }
}
