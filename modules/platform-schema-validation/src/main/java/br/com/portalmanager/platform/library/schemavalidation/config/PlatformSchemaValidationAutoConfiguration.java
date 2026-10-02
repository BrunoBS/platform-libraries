package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.schemavalidation.aspect.ResourceSchemaValidationAspect;
import br.com.portalmanager.platform.library.schemavalidation.cache.*;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.resolver.DefaultResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
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
@ConditionalOnBean(ResourceSchemaRepository.class)
public class PlatformSchemaValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaCache resourceSchemaCache() {
        return new NoOpResourceSchemaCache();
    }

    @Bean
    @ConditionalOnMissingBean
    CompiledSchemaCache compiledSchemaCache(PlatformSchemaValidationProperties properties) {
        var local = properties.getCache().getLocal();
        return local.isEnabled()
                ? new CaffeineCompiledSchemaCache(local.getTtl(), local.getMaxSize())
                : new NoOpCompiledSchemaCache();
    }

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaResolver resourceSchemaResolver(
            ResourceSchemaRepository repository,
            PlatformSchemaValidationProperties properties,
            ResourceSchemaCache cache
    ) {
        return new DefaultResourceSchemaResolver(repository, properties, cache);
    }

    @Bean
    @ConditionalOnMissingBean
    SchemaValidator schemaValidator(
            ResourceSchemaResolver resolver,
            ObjectMapper objectMapper,
            CompiledSchemaCache compiledSchemaCache
    ) {
        return new ResourceSchemaValidator(resolver, objectMapper, compiledSchemaCache);
    }

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaValidationAspect resourceSchemaValidationAspect(
            SchemaValidator validator,
            ObjectMapper objectMapper
    ) {
        return new ResourceSchemaValidationAspect(validator, objectMapper);
    }
}
