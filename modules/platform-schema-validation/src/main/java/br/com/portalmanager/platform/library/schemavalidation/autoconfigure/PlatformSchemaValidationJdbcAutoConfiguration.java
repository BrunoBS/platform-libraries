package br.com.portalmanager.platform.library.schemavalidation.autoconfigure;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.config.SqlIdentifierValidator;
import br.com.portalmanager.platform.library.schemavalidation.repository.JdbcResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration(
        after = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class},
        before = PlatformSchemaValidationAutoConfiguration.class
)
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(
        prefix = "platform.schema-validation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnProperty(
        prefix = "platform.schema-validation.datasource",
        name = "enabled",
        havingValue = "true"
)
public class PlatformSchemaValidationJdbcAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean(ResourceSchemaRepository.class)
    ResourceSchemaRepository jdbcResourceSchemaRepository(
            JdbcTemplate jdbcTemplate,
            PlatformSchemaValidationProperties properties
    ) {
        SqlIdentifierValidator.validate(properties.getDatasource().getViewName());
        return new JdbcResourceSchemaRepository(jdbcTemplate, properties);
    }
}
