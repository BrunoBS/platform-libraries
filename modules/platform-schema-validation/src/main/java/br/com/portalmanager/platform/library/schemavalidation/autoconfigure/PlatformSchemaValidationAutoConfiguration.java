package br.com.portalmanager.platform.library.schemavalidation.autoconfigure;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.repository.NoOpResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.resolver.DefaultResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.web.ResourceSchemaRequestBodyAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformSchemaValidationProperties.class)
@ConditionalOnProperty(
        prefix = "platform.schema-validation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PlatformSchemaValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ResourceSchemaRepository.class)
    ResourceSchemaRepository noOpResourceSchemaRepository() {
        return new NoOpResourceSchemaRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaResolver resourceSchemaResolver(
            ResourceSchemaRepository repository,
            PlatformSchemaValidationProperties properties
    ) {
        return new DefaultResourceSchemaResolver(repository, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaValidator resourceSchemaValidator(
            ResourceSchemaResolver resolver,
            ObjectMapper objectMapper
    ) {
        return new ResourceSchemaValidator(resolver, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaRequestBodyAdvice resourceSchemaRequestBodyAdvice(
            ResourceSchemaValidator validator,
            ObjectMapper objectMapper
    ) {
        return new ResourceSchemaRequestBodyAdvice(validator, objectMapper);
    }
}
