package br.com.portalmanager.platform.library.messaging.config;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.repository.ApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.repository.JdbcApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.repository.NoOpApiMessageRepository;
import br.com.portalmanager.platform.library.messaging.resolver.ApiMessageResolver;
import br.com.portalmanager.platform.library.messaging.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformMessagingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformMessagingJdbcAutoConfiguration.class, PlatformMessagingAutoConfiguration.class));

    @Configuration
    static class MockInfrastructureConfiguration {
        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }
    }

    @Test
    void shouldNotLoadBeansWhenDisabled() {
        this.contextRunner
                .withUserConfiguration(MockInfrastructureConfiguration.class)
                .withPropertyValues("platform.messaging.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ApiMessageResolver.class);
                    assertThat(context).doesNotHaveBean(ApiExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(ApiMessageRepository.class);
                });
    }

    @Test
    void shouldUseNoOpRepositoryByDefaultEvenWhenJdbcTemplateExists() {
        this.contextRunner
                .withUserConfiguration(MockInfrastructureConfiguration.class)
                .withPropertyValues(
                        "platform.messaging.enabled=true",
                        "platform.messaging.default-locale=pt-BR",
                        "spring.application.name=test-service"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ApiMessageRepository.class);
                    assertThat(context.getBean(ApiMessageRepository.class))
                            .isInstanceOf(NoOpApiMessageRepository.class);
                    assertThat(context).hasSingleBean(ApiMessageResolver.class);
                    assertThat(context).hasSingleBean(ApiExceptionHandler.class);
                });
    }

    @Test
    void shouldUseJdbcRepositoryWhenExplicitlyEnabled() {
        this.contextRunner
                .withUserConfiguration(MockInfrastructureConfiguration.class)
                .withPropertyValues(
                        "platform.messaging.enabled=true",
                        "platform.messaging.datasource.enabled=true",
                        "platform.messaging.default-locale=pt-BR",
                        "spring.application.name=test-service"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ApiMessageRepository.class);
                    assertThat(context.getBean(ApiMessageRepository.class))
                            .isInstanceOf(JdbcApiMessageRepository.class);
                    assertThat(context).hasSingleBean(ApiMessageResolver.class);
                    assertThat(context).hasSingleBean(ApiExceptionHandler.class);
                });
    }

    @Test
    void shouldUseNoOpRepositoryWhenJdbcTemplateDoesNotExist() {
        this.contextRunner
                .withPropertyValues(
                        "platform.messaging.enabled=true",
                        "platform.messaging.default-locale=pt-BR",
                        "spring.application.name=test-service"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ApiMessageRepository.class);
                    assertThat(context.getBean(ApiMessageRepository.class))
                            .isInstanceOf(NoOpApiMessageRepository.class);
                    assertThat(context).hasSingleBean(ApiMessageResolver.class);
                    assertThat(context).hasSingleBean(ApiExceptionHandler.class);
                });
    }
    @Test
    void shouldFailStartupWhenApplicationNameIsMissing() {
        this.contextRunner
                .withUserConfiguration(MockInfrastructureConfiguration.class)
                .withPropertyValues(
                        "platform.messaging.enabled=true",
                        "platform.messaging.default-locale=pt-BR"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(PlatformConfigurationException.class)
                            .hasRootCauseMessage(
                                    "spring.application.name is required when platform.messaging is enabled"
                            );

                    PlatformConfigurationException exception =
                            (PlatformConfigurationException) context.getStartupFailure().getCause().getCause();

                    assertThat(exception.getErrorResponse().code()).isEqualTo("PLT-MSG-001");
                    assertThat(exception.getErrorResponse().message())
                            .isEqualTo("spring.application.name is required when platform.messaging is enabled");
                    assertThat(exception.getErrorResponse().solution())
                            .isEqualTo("Configure spring.application.name with the service name.");
                    assertThat(exception.getErrorResponse().details()).isEmpty();
                    assertThat(exception.getErrorResponse().path()).isNull();
                    assertThat(exception.getErrorResponse().correlationId()).isNull();
                });
    }

    @Test
    void shouldFailStartupWhenApplicationNameIsBlank() {
        this.contextRunner
                .withUserConfiguration(MockInfrastructureConfiguration.class)
                .withPropertyValues(
                        "platform.messaging.enabled=true",
                        "platform.messaging.default-locale=pt-BR",
                        "spring.application.name=   "
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(PlatformConfigurationException.class)
                            .hasRootCauseMessage(
                                    "spring.application.name is required when platform.messaging is enabled"
                            );

                    PlatformConfigurationException exception =
                            (PlatformConfigurationException) context.getStartupFailure().getCause().getCause();

                    assertThat(exception.getErrorResponse().code()).isEqualTo("PLT-MSG-001");
                    assertThat(exception.getErrorResponse().message())
                            .isEqualTo("spring.application.name is required when platform.messaging is enabled");
                    assertThat(exception.getErrorResponse().solution())
                            .isEqualTo("Configure spring.application.name with the service name.");
                    assertThat(exception.getErrorResponse().details()).isEmpty();
                    assertThat(exception.getErrorResponse().path()).isNull();
                    assertThat(exception.getErrorResponse().correlationId()).isNull();
                });
    }
}
