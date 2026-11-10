package br.com.portalmanager.platform.library.schemavalidation.configuration;

import br.com.portalmanager.platform.library.schemavalidation.api.configuration.PlatformSchemaValidationProperties;

import br.com.portalmanager.platform.library.messaging.api.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.schemavalidation.internal.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.repository.ResourceSchemaRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(
        after = PlatformSchemaValidationJdbcAutoConfiguration.class,
        before = PlatformSchemaValidationAutoConfiguration.class
)
@EnableConfigurationProperties(PlatformSchemaValidationProperties.class)
public class PlatformSchemaValidationSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ResourceSchemaRepository.class)
    Object schemaValidationSourceRequired(PlatformSchemaValidationProperties properties) {
        throw new PlatformConfigurationException(
                SchemaValidationTechnicalErrors.schemaSourceMissing(
                        properties.resolveViewName()
                )
        );
    }
}
