package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.web.ResourceSchemaRequestBodyAdvice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = PlatformSchemaValidationAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestBodyAdviceAdapter.class)
@ConditionalOnBean(SchemaValidator.class)
public class PlatformSchemaValidationWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ResourceSchemaRequestBodyAdvice resourceSchemaRequestBodyAdvice(
        ObjectMapper objectMapper,
        SchemaValidator schemaValidator,
        BeanFactory beanFactory
    ) {
        return new ResourceSchemaRequestBodyAdvice(objectMapper, schemaValidator, beanFactory);
    }
}
