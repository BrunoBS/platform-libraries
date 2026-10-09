package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.aop.AuthorizationFacadeAspect;
import br.com.portalmanager.platform.library.authorization.aop.MockAuthorizationFacadeAspect;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformAuthorizationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformAuthorizationAutoConfiguration.class));

    @Configuration
    static class InfrastructureConfiguration {
        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

    @Test
    void shouldLoadRealSecurityBeansInRealMode() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
                    assertThat(context).hasSingleBean(AuthorizationClient.class);
                    assertThat(context).hasSingleBean(AuthorizationFacadeAspect.class);
                    assertThat(context).doesNotHaveBean(MockAuthorizationFacadeAspect.class);
                });
    }

    @Test
    void shouldUseRealModeByDefaultWhenPropertyIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues("platform.authorization.service-url=http://localhost")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationClient.class);
                    assertThat(context).hasSingleBean(AuthorizationFacadeAspect.class);
                    assertThat(context).doesNotHaveBean(MockAuthorizationFacadeAspect.class);
                });
    }

    @Test
    void shouldFailWhenServiceUrlIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues("platform.authorization.mode=REAL")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(PlatformConfigurationException.class);
                });
    }

    @Test
    void shouldFailWhenServiceUrlIsBlank() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(PlatformConfigurationException.class);
                });
    }

    @Test
    void shouldLoadMockAspectWithoutRealClient() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues("platform.authorization.mode=MOCK").withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("test"))
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
                    assertThat(context).hasSingleBean(MockAuthorizationFacadeAspect.class);
                    assertThat(context).doesNotHaveBean(AuthorizationClient.class);
                    assertThat(context).doesNotHaveBean(AuthorizationFacadeAspect.class);
                });
    }

    @Test
    void shouldRejectMockWithoutActiveProfile() {
        contextRunner.withPropertyValues("platform.authorization.mode=MOCK")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isInstanceOf(PlatformConfigurationException.class);
                });
    }

    @Test
    void shouldRejectMockInProduction() {
        contextRunner.withPropertyValues("platform.authorization.mode=MOCK")
                .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("prod"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isInstanceOf(PlatformConfigurationException.class);
                });
    }

    @Test
    void shouldRejectMockWithMixedProfiles() {
        contextRunner.withPropertyValues("platform.authorization.mode=MOCK")
                .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("local", "prod"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isInstanceOf(PlatformConfigurationException.class);
                });
    }

    @Test
    void shouldAllowMockWithLocalProfile() {
        contextRunner.withPropertyValues("platform.authorization.mode=MOCK")
                .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("local"))
                .run(context -> assertThat(context).hasSingleBean(MockAuthorizationFacadeAspect.class));
    }

    @Test
    void shouldAllowMockWithDevProfile() {
        contextRunner.withPropertyValues("platform.authorization.mode=MOCK")
                .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("dev"))
                .run(context -> assertThat(context).hasSingleBean(MockAuthorizationFacadeAspect.class));
    }

    @Test
    void shouldKeepUserAuthorizationMetadataRegistry() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomRegistryConfiguration.class)
                .withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
                    assertThat(context.getBean(AuthorizationMetadataRegistry.class))
                            .isSameAs(context.getBean("customAuthorizationMetadataRegistry"));
                });
    }

    @Test
    void shouldKeepUserAuthorizationClient() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomClientConfiguration.class)
                .withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationClient.class);
                    assertThat(context.getBean(AuthorizationClient.class))
                            .isSameAs(context.getBean("customAuthorizationClient"));
                    assertThat(context).hasSingleBean(AuthorizationFacadeAspect.class);
                });
    }

    @Test
    void shouldKeepUserAuthorizationFacadeAspect() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomAspectConfiguration.class)
                .withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuthorizationFacadeAspect.class);
                    assertThat(context.getBean(AuthorizationFacadeAspect.class))
                            .isSameAs(context.getBean("customAuthorizationFacadeAspect"));
                });
    }

    @Configuration
    static class CustomRegistryConfiguration {
        @Bean
        AuthorizationMetadataRegistry customAuthorizationMetadataRegistry() {
            return mock(AuthorizationMetadataRegistry.class);
        }
    }

    @Configuration
    static class CustomClientConfiguration {
        @Bean
        AuthorizationClient customAuthorizationClient() {
            return mock(AuthorizationClient.class);
        }
    }

    @Configuration
    static class CustomAspectConfiguration {
        @Bean
        AuthorizationFacadeAspect customAuthorizationFacadeAspect() {
            return mock(AuthorizationFacadeAspect.class);
        }
    }
}
