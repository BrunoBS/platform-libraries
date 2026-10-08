package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.schemavalidation.aspect.ResourceSchemaValidationAspect;
import br.com.portalmanager.platform.library.schemavalidation.repository.JdbcResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformSchemaValidationAutoConfigurationTest {

    @Test
    void shouldUseCustomRepositoryAndNotCreateJdbcRepository() {
        try (AnnotationConfigApplicationContext context = openContext(
                PlatformSchemaValidationJdbcAutoConfiguration.class,
                PlatformSchemaValidationSourceAutoConfiguration.class,
                PlatformSchemaValidationAutoConfiguration.class,
                CustomRepositoryConfiguration.class
        )) {
            assertThat(context.getBeansOfType(ResourceSchemaRepository.class)).hasSize(1);
            assertThat(context.getBean(ResourceSchemaRepository.class))
                    .isSameAs(CustomRepositoryConfiguration.REPOSITORY);
            assertThat(context.getBeansOfType(JdbcResourceSchemaRepository.class)).isEmpty();
            assertThat(context.getBeansOfType(ResourceSchemaResolver.class)).hasSize(1);
            assertThat(context.getBeansOfType(SchemaValidator.class)).hasSize(1);
            assertThat(context.getBeansOfType(ResourceSchemaValidationAspect.class)).hasSize(1);
        }
    }

    @Test
    void shouldCreateJdbcRepositoryWhenJdbcTemplateIsAvailable() {
        try (AnnotationConfigApplicationContext context = openContext(
                PlatformSchemaValidationJdbcAutoConfiguration.class,
                PlatformSchemaValidationSourceAutoConfiguration.class,
                PlatformSchemaValidationAutoConfiguration.class,
                JdbcConfiguration.class
        )) {
            assertThat(context.getBeansOfType(ResourceSchemaRepository.class)).hasSize(1);
            assertThat(context.getBean(ResourceSchemaRepository.class))
                    .isInstanceOf(JdbcResourceSchemaRepository.class);
            assertThat(context.getBeansOfType(ResourceSchemaResolver.class)).hasSize(1);
            assertThat(context.getBeansOfType(SchemaValidator.class)).hasSize(1);
            assertThat(context.getBeansOfType(ResourceSchemaValidationAspect.class)).hasSize(1);
        }
    }

    @Test
    void shouldUseConfiguredViewNameInJdbcSourceValidation() {
        try (AnnotationConfigApplicationContext context = openContext(
                Map.of("platform.schema-validation.view-name", "vw_custom_resource_schemas"),
                PlatformSchemaValidationJdbcAutoConfiguration.class,
                PlatformSchemaValidationSourceAutoConfiguration.class,
                PlatformSchemaValidationAutoConfiguration.class,
                JdbcConfiguration.class
        )) {
            org.mockito.Mockito.verify(JdbcConfiguration.JDBC_TEMPLATE).query(
                    org.mockito.ArgumentMatchers.<String>argThat(sql ->
                            sql.contains("FROM vw_custom_resource_schemas")
                                    && sql.contains("WHERE 1 = 0")
                    ),
                    org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.ResultSetExtractor<Object>>any()
            );
        }
    }

    @Test
    void shouldFailStartupWithoutRepositoryOrJdbcTemplate() {
        Throwable failure = catchThrowable(() -> openContext(
                PlatformSchemaValidationJdbcAutoConfiguration.class,
                PlatformSchemaValidationSourceAutoConfiguration.class,
                PlatformSchemaValidationAutoConfiguration.class
        ));

        assertThat(failure)
                .hasRootCauseInstanceOf(PlatformConfigurationException.class);

        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        assertThat(cause.getMessage())
                .contains("vw_platform_resource_schemas")
                .contains("resource_type, resource_code, schema_version, definition")
                .contains("platform.schema-validation.view-name")
                .contains("ResourceSchemaRepository");
    }

    @Test
    void shouldExposePublicSchemaValidatorContractForDirectValidation() {
        try (AnnotationConfigApplicationContext context = openContext(
                PlatformSchemaValidationAutoConfiguration.class,
                ValidatingRepositoryConfiguration.class
        )) {
            SchemaValidator validator = context.getBean(SchemaValidator.class);

            validator.validate(
                    "PUBLISHER",
                    "websocket",
                    context.getBean(ObjectMapper.class).createObjectNode().put("url", "wss://example.test")
            );

            assertThat(validator).isNotNull();
        }
    }

    @Test
    void shouldCreateValidatorAndAspectOnlyWhenRepositoryDependencyExists() {
        try (AnnotationConfigApplicationContext context = openContext(
                PlatformSchemaValidationAutoConfiguration.class,
                CustomRepositoryWithoutJdbcConfiguration.class
        )) {
            assertThat(context.getBeansOfType(ResourceSchemaRepository.class)).hasSize(1);
            assertThat(context.getBeansOfType(ResourceSchemaResolver.class)).hasSize(1);
            assertThat(context.getBeansOfType(SchemaValidator.class)).hasSize(1);
            assertThat(context.getBeansOfType(ResourceSchemaValidationAspect.class)).hasSize(1);
        }
    }

    private AnnotationConfigApplicationContext openContext(Class<?>... configurationClasses) {
        return openContext(Map.of(), configurationClasses);
    }

    private AnnotationConfigApplicationContext openContext(
            Map<String, Object> properties,
            Class<?>... configurationClasses
    ) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        if (!properties.isEmpty()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test-properties", properties)
            );
        }
        context.register(configurationClasses);
        try {
            context.refresh();
            return context;
        } catch (RuntimeException exception) {
            context.close();
            throw exception;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRepositoryConfiguration {

        private static final ResourceSchemaRepository REPOSITORY = mock(ResourceSchemaRepository.class);

        @Bean
        ResourceSchemaRepository resourceSchemaRepository() {
            return REPOSITORY;
        }

        @Bean
        JdbcTemplate jdbcTemplate() {
            JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
            when(jdbcTemplate.query(
                    any(String.class),
                    any(org.springframework.jdbc.core.ResultSetExtractor.class)
            )).thenReturn(null);
            return jdbcTemplate;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class JdbcConfiguration {

        private static final JdbcTemplate JDBC_TEMPLATE = mock(JdbcTemplate.class);

        @Bean
        JdbcTemplate jdbcTemplate() {
            return JDBC_TEMPLATE;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ValidatingRepositoryConfiguration {

        @Bean
        ResourceSchemaRepository resourceSchemaRepository() {
            return (resourceType, resourceCode) -> java.util.Optional.of(
                    new br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema(
                            resourceType,
                            resourceCode,
                            1,
                            """
                            {
                              "type": "object",
                              "properties": {
                                "url": { "type": "string" }
                              },
                              "required": ["url"]
                            }
                            """
                    )
            );
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomRepositoryWithoutJdbcConfiguration {

        @Bean
        ResourceSchemaRepository resourceSchemaRepository() {
            return mock(ResourceSchemaRepository.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
