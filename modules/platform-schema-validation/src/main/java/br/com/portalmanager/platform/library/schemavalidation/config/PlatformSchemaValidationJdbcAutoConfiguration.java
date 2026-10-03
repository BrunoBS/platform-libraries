package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.schemavalidation.repository.JdbcResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration(
        after = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class},
        before = PlatformSchemaValidationAutoConfiguration.class
)
@ConditionalOnClass(JdbcTemplate.class)
@EnableConfigurationProperties(PlatformSchemaValidationProperties.class)
public class PlatformSchemaValidationJdbcAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean(ResourceSchemaRepository.class)
    ResourceSchemaRepository jdbcResourceSchemaRepository(
            JdbcTemplate jdbcTemplate,
            PlatformSchemaValidationProperties properties
    ) {
        SqlIdentifierValidator.validate(properties.resolveViewName());

        JdbcResourceSchemaRepository repository =
                new JdbcResourceSchemaRepository(jdbcTemplate, properties);
        repository.validateSource();

        return repository;
    }
}
