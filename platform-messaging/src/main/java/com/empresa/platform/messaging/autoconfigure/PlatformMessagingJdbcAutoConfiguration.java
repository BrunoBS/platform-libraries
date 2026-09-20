package com.empresa.platform.messaging.autoconfigure;

import com.empresa.platform.messaging.config.PlatformMessagingProperties;
import com.empresa.platform.messaging.config.SqlIdentifierValidator;
import com.empresa.platform.messaging.repository.ApiMessageRepository;
import com.empresa.platform.messaging.repository.JdbcApiMessageRepository;
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
