package com.empresa.platform.crud.autoconfigure;

import com.empresa.platform.messaging.autoconfigure.PlatformMessagingAutoConfiguration;
import com.empresa.platform.messaging.web.ApiExceptionHandler;
import com.empresa.platform.crud.web.CrudValidationExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = PlatformMessagingAutoConfiguration.class)
public class PlatformCrudAutoConfiguration {

    @Bean
    @ConditionalOnBean(ApiExceptionHandler.class)
    CrudValidationExceptionHandler crudValidationExceptionHandler(
            ApiExceptionHandler apiExceptionHandler
    ) {
        return new CrudValidationExceptionHandler(apiExceptionHandler);
    }
}
