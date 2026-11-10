package br.com.portalmanager.platform.library.schemavalidation.configuration;

import br.com.portalmanager.platform.library.schemavalidation.internal.aspect.ResourceSchemaValidationAspect;
import br.com.portalmanager.platform.library.schemavalidation.api.configuration.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.cache.CompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.cache.ResourceSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.internal.cache.CaffeineCompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.internal.cache.NoOpCompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.internal.cache.NoOpResourceSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.internal.resolver.DefaultResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.internal.validation.JsonSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.internal.validation.ResourceSchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.validation.SchemaValidator;

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
