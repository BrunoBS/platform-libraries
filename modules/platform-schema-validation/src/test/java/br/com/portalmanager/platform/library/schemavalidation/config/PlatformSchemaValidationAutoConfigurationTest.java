package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.schemavalidation.aspect.ResourceSchemaValidationAspect;
import br.com.portalmanager.platform.library.schemavalidation.repository.JdbcResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformSchemaValidationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PlatformSchemaValidationJdbcAutoConfiguration.class,
                    PlatformSchemaValidationSourceAutoConfiguration.class,
                    PlatformSchemaValidationAutoConfiguration.class
            ));

    @Test
    void shouldUseCustomRepositoryAndNotCreateJdbcRepository() {
        contextRunner
                .withUserConfiguration(CustomRepositoryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceSchemaRepository.class);
                    assertThat(context.getBean(ResourceSchemaRepository.class))
                            .isSameAs(CustomRepositoryConfiguration.REPOSITORY);
                    assertThat(context).doesNotHaveBean(JdbcResourceSchemaRepository.class);
                    assertThat(context).hasSingleBean(ResourceSchemaResolver.class);
                    assertThat(context).hasSingleBean(SchemaValidator.class);
                    assertThat(context).hasSingleBean(ResourceSchemaValidationAspect.class);
                });
    }

    @Test
    void shouldCreateJdbcRepositoryWhenJdbcTemplateIsAvailable() {
        contextRunner
                .withUserConfiguration(JdbcConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceSchemaRepository.class);
                    assertThat(context.getBean(ResourceSchemaRepository.class))
                            .isInstanceOf(JdbcResourceSchemaRepository.class);
                    assertThat(context).hasSingleBean(ResourceSchemaResolver.class);
                    assertThat(context).hasSingleBean(SchemaValidator.class);
                    assertThat(context).hasSingleBean(ResourceSchemaValidationAspect.class);
                });
    }

    @Test
    void shouldFailStartupWithoutRepositoryOrJdbcTemplate() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(PlatformConfigurationException.class);

            Throwable cause = context.getStartupFailure();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            assertThat(cause.getMessage())
                    .contains("vw_platform_resource_schemas")
                    .contains("resource_type, resource_code, schema_version, definition")
                    .contains("platform.schema-validation.view-name")
                    .contains("ResourceSchemaRepository");
        });
    }

    @Test
    void shouldExposePublicSchemaValidatorContractForDirectValidation() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformSchemaValidationAutoConfiguration.class))
                .withUserConfiguration(ValidatingRepositoryConfiguration.class)
                .run(context -> {
                    SchemaValidator validator = context.getBean(SchemaValidator.class);

                    validator.validate(
                            "PUBLISHER",
                            "websocket",
                            context.getBean(ObjectMapper.class).createObjectNode().put("url", "wss://example.test")
                    );

                    assertThat(validator).isNotNull();
                });
    }

    @Test
    void shouldCreateValidatorAndAspectOnlyWhenRepositoryDependencyExists() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformSchemaValidationAutoConfiguration.class))
                .withUserConfiguration(CustomRepositoryWithoutJdbcConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceSchemaRepository.class);
                    assertThat(context).hasSingleBean(ResourceSchemaResolver.class);
                    assertThat(context).hasSingleBean(SchemaValidator.class);
                    assertThat(context).hasSingleBean(ResourceSchemaValidationAspect.class);
                });
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

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
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
