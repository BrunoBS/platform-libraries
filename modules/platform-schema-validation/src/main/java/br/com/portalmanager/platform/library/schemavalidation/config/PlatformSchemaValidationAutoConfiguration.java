package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.schemavalidation.aspect.ResourceSchemaValidationAspect;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.resolver.DefaultResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformSchemaValidationProperties.class)
public class PlatformSchemaValidationAutoConfiguration {

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
    ResourceSchemaValidationAspect resourceSchemaValidationAspect(
            ResourceSchemaValidator validator,
            ObjectMapper objectMapper
    ) {
        return new ResourceSchemaValidationAspect(validator, objectMapper);
    }
}
