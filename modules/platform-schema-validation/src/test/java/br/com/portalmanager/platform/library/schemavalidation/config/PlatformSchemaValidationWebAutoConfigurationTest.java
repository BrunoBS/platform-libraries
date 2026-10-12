package br.com.portalmanager.platform.library.schemavalidation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.library.schemavalidation.web.ResourceSchemaRequestBodyAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import tools.jackson.databind.ObjectMapper;

class PlatformSchemaValidationWebAutoConfigurationTest {

    @Test
    void registersAdviceWhenObjectMapperIsDefinedAfterWebConfiguration() {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(
                ValidatorBeans.class,
                PlatformSchemaValidationWebAutoConfiguration.class,
                MapperBeans.class
            );
            context.refresh();
            assertThat(context.getBeansOfType(ResourceSchemaRequestBodyAdvice.class)).hasSize(1);
        }
    }

    @Test
    void registersAdviceInServletApplicationsWithValidator() {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(Beans.class, PlatformSchemaValidationWebAutoConfiguration.class);
            context.refresh();
            assertThat(context.getBeansOfType(ResourceSchemaRequestBodyAdvice.class)).hasSize(1);
        }
    }

    @Test
    void doesNotRegisterAdviceOutsideWebApplications() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(Beans.class, PlatformSchemaValidationWebAutoConfiguration.class);
            context.refresh();
            assertThat(context.getBeansOfType(ResourceSchemaRequestBodyAdvice.class)).isEmpty();
        }
    }

    @Test
    void doesNotRegisterAdviceWithoutValidator() {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(PlatformSchemaValidationWebAutoConfiguration.class);
            context.refresh();
            assertThat(context.getBeansOfType(ResourceSchemaRequestBodyAdvice.class)).isEmpty();
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class Beans {

        @org.springframework.context.annotation.Bean
        SchemaValidator schemaValidator() {
            return mock(SchemaValidator.class);
        }

        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class ValidatorBeans {

        @org.springframework.context.annotation.Bean
        SchemaValidator schemaValidator() {
            return mock(SchemaValidator.class);
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class MapperBeans {

        @org.springframework.context.annotation.Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
