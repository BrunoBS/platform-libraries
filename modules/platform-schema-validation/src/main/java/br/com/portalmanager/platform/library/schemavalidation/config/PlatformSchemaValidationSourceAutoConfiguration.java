package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(
        after = PlatformSchemaValidationJdbcAutoConfiguration.class,
        before = PlatformSchemaValidationAutoConfiguration.class
)
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
