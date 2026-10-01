package br.com.portalmanager.platform.library.messaging.config;

import br.com.portalmanager.platform.library.messaging.config.PlatformMessagingProperties;
import br.com.portalmanager.platform.library.messaging.config.SqlIdentifierValidator;
import br.com.portalmanager.platform.library.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.repository.JdbcApiMessageRepository;
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
        before = PlatformMessagingAutoConfiguration.class
)
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnProperty(prefix = "platform.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "platform.messaging.datasource", name = "enabled", havingValue = "true")
public class PlatformMessagingJdbcAutoConfiguration {

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean(ApiMessageRepository.class)
    ApiMessageRepository jdbcApiMessageRepository(
            JdbcTemplate jdbc,
            PlatformMessagingProperties properties
    ) {
        SqlIdentifierValidator.validate(properties.getDatasource().getViewName());
        return new JdbcApiMessageRepository(jdbc, properties);
    }
}
