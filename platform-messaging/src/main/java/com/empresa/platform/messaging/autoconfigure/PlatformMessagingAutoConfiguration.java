package com.empresa.platform.messaging.autoconfigure;

import com.empresa.platform.messaging.cache.ApiMessageCache;
import com.empresa.platform.messaging.cache.NoOpApiMessageCache;
import com.empresa.platform.messaging.config.PlatformMessagingProperties;
import com.empresa.platform.messaging.config.SqlIdentifierValidator;
import com.empresa.platform.messaging.message.PlatformDefaultMessageProvider;
import com.empresa.platform.messaging.provider.ApiMessageProvider;
import com.empresa.platform.messaging.repository.ApiMessageRepository;
import com.empresa.platform.messaging.repository.JdbcApiMessageRepository;
import com.empresa.platform.messaging.resolver.ApiMessageResolver;
import com.empresa.platform.messaging.resolver.DefaultApiMessageResolver;
import com.empresa.platform.messaging.web.ApiExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Locale;

@AutoConfiguration
@EnableConfigurationProperties(PlatformMessagingProperties.class)
@ConditionalOnProperty(prefix = "platform.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ApiMessageRepository apiMessageRepository(JdbcTemplate jdbc, PlatformMessagingProperties p) {
        SqlIdentifierValidator.validate(p.getDatasource().getViewName());
        return new JdbcApiMessageRepository(jdbc, p);
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
