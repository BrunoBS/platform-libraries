package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.schemavalidation.aspect.ResourceSchemaValidationAspect;
import br.com.portalmanager.platform.library.schemavalidation.cache.*;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.resolver.DefaultResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.validation.JsonSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformSchemaValidationProperties.class)
public class PlatformSchemaValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaCache resourceSchemaCache() {
        return new NoOpResourceSchemaCache();
    }

    @Bean
    @ConditionalOnMissingBean
    CompiledSchemaCache compiledSchemaCache(PlatformSchemaValidationProperties properties) {
        PlatformSchemaValidationProperties.Local local = properties.getCache().getLocal();
        return local.isEnabled()
                ? new CaffeineCompiledSchemaCache(local.getTtl(), local.getMaxSize())
                : new NoOpCompiledSchemaCache();
    }

    @Bean
    @ConditionalOnBean(ResourceSchemaRepository.class)
    @ConditionalOnMissingBean
    ResourceSchemaResolver resourceSchemaResolver(
            ResourceSchemaRepository repository,
            PlatformSchemaValidationProperties properties,
            ResourceSchemaCache cache) {
        return new DefaultResourceSchemaResolver(repository, properties, cache);
    }

    @Bean
    @ConditionalOnMissingBean
    JsonSchemaValidator jsonSchemaValidator(
            ObjectMapper objectMapper, CompiledSchemaCache compiledSchemaCache) {
        return new JsonSchemaValidator(objectMapper, compiledSchemaCache);
    }

    @Bean
    @ConditionalOnBean(ResourceSchemaResolver.class)
    @ConditionalOnMissingBean
    SchemaValidator schemaValidator(
            ResourceSchemaResolver resolver, JsonSchemaValidator validator) {
        return new ResourceSchemaValidator(resolver, validator);
    }

    @Bean
    @ConditionalOnBean(SchemaValidator.class)
    @ConditionalOnMissingBean
    ResourceSchemaValidationAspect resourceSchemaValidationAspect(
            SchemaValidator validator, ObjectMapper objectMapper) {
        return new ResourceSchemaValidationAspect(validator, objectMapper);
    }
}
