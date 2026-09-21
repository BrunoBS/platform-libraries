package br.com.portalmanager.platform.messaging.autoconfigure;

import br.com.portalmanager.platform.messaging.cache.ApiMessageCache;
import br.com.portalmanager.platform.messaging.cache.NoOpApiMessageCache;
import br.com.portalmanager.platform.messaging.config.PlatformMessagingProperties;
import br.com.portalmanager.platform.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.messaging.provider.ApiMessageProvider;
import br.com.portalmanager.platform.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.messaging.repository.NoOpApiMessageRepository;
import br.com.portalmanager.platform.messaging.resolver.ApiMessageResolver;
import br.com.portalmanager.platform.messaging.resolver.DefaultApiMessageResolver;
import br.com.portalmanager.platform.messaging.web.ApiExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

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
    ApiMessageProvider apiMessageProvider() {
        return new PlatformDefaultMessageProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    ApiMessageResolver apiMessageResolver(
            ApiMessageRepository repository,
            ApiMessageCache cache,
            PlatformMessagingProperties properties,
            ApiMessageProvider provider
    ) {
        Locale yamlLocale = Locale.forLanguageTag(properties.getDefaultLocale());
        Locale safeDefault = yamlLocale.equals(Locale.ROOT)
                ? Locale.forLanguageTag("pt-BR")
                : yamlLocale;
        return new DefaultApiMessageResolver(repository, cache, safeDefault, provider);
    }

    @Bean
    @ConditionalOnMissingBean
    ApiExceptionHandler apiExceptionHandler(ApiMessageResolver resolver) {
        return new ApiExceptionHandler(resolver);
    }
}
