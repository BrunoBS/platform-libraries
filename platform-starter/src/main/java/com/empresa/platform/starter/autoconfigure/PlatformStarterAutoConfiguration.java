package com.empresa.platform.starter.autoconfigure;

import com.empresa.platform.messaging.web.ApiExceptionHandler;
import com.empresa.platform.starter.web.CrudValidationExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PlatformStarterAutoConfiguration {

    @Bean
    CrudValidationExceptionHandler crudValidationExceptionHandler(
            ApiExceptionHandler apiExceptionHandler
    ) {
        return new CrudValidationExceptionHandler(apiExceptionHandler);
    }
}
