package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.schemavalidation.aspect.ResourceSchemaValidationAspect;
import br.com.portalmanager.platform.library.schemavalidation.repository.JdbcResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.validation.ResourceSchemaValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformSchemaValidationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PlatformSchemaValidationJdbcAutoConfiguration.class,
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
                    assertThat(context).hasSingleBean(ResourceSchemaValidator.class);
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
                    assertThat(context).hasSingleBean(ResourceSchemaValidator.class);
                    assertThat(context).hasSingleBean(ResourceSchemaValidationAspect.class);
                });
    }

    @Test
    void shouldNotActivateRuntimeWithoutRepositoryOrJdbcTemplate() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ResourceSchemaRepository.class);
            assertThat(context).doesNotHaveBean(ResourceSchemaResolver.class);
            assertThat(context).doesNotHaveBean(ResourceSchemaValidator.class);
            assertThat(context).doesNotHaveBean(ResourceSchemaValidationAspect.class);
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
                    assertThat(context).hasSingleBean(ResourceSchemaValidator.class);
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
            return mock(JdbcTemplate.class);
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
