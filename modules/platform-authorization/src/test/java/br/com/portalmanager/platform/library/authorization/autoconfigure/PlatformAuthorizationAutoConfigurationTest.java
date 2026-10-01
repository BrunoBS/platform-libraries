package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationClientService;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationInterceptor;
import br.com.portalmanager.platform.library.authorization.web.MockAuthorizationInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformAuthorizationAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PlatformAuthorizationAutoConfiguration.class));

    @AfterEach
    void tearDown() {
        UserContext.clear();
        org.slf4j.MDC.clear();
    }

    @Configuration
    static class InfrastructureConfiguration {

        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

    @Test
    void shouldLoadRealSecurityBeansInRealMode() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
            assertThat(context).hasSingleBean(AuthorizationClientService.class);
            assertThat(context).hasSingleBean(AuthorizationInterceptor.class);
            assertThat(context).hasSingleBean(WebMvcConfigurer.class);
            assertThat(context).doesNotHaveBean("mockInterceptorConfigurer");
        });
    }

    @Test
    void shouldUseRealModeByDefaultWhenPropertyIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationClientService.class);
            assertThat(context).hasSingleBean(AuthorizationInterceptor.class);
            assertThat(context).hasSingleBean(WebMvcConfigurer.class);
        });
    }

    @Test
    void shouldFailWhenServiceUrlIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=REAL").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(PlatformConfigurationException.class);
            assertThat(context.getStartupFailure()).hasRootCauseMessage("platform.authorization.service-url is required when platform.authorization.mode=REAL");
        });
    }

    @Test
    void shouldFailWhenServiceUrlIsBlank() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(PlatformConfigurationException.class);
        });
    }

    @Test
    void shouldLoadMockGuestInterceptorInMockMode() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=MOCK").run(context -> {
            assertThat(context).doesNotHaveBean(AuthorizationClientService.class);
            assertThat(context).doesNotHaveBean(AuthorizationInterceptor.class);
            assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
            assertThat(context).hasSingleBean(MockAuthorizationInterceptor.class);
            assertThat(context).hasSingleBean(WebMvcConfigurer.class);
        });
    }

    @Test
    void shouldNotCreateRealSecurityBeansInMockMode() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=MOCK").run(context -> {
            assertThat(context).doesNotHaveBean(AuthorizationClientService.class);
            assertThat(context).doesNotHaveBean(AuthorizationInterceptor.class);
        });
    }

    @Test
    void shouldKeepUserAuthorizationMetadataRegistry() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomRegistryConfiguration.class).withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
            assertThat(context.getBean(AuthorizationMetadataRegistry.class)).isSameAs(context.getBean("customAuthorizationMetadataRegistry"));
        });
    }

    @Test
    void shouldKeepUserAuthorizationClientService() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomClientServiceConfiguration.class).withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationClientService.class);
            assertThat(context.getBean(AuthorizationClientService.class)).isSameAs(context.getBean("customAuthorizationClientService"));
        });
    }

    @Test
    void shouldKeepUserAuthorizationInterceptor() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomInterceptorConfiguration.class).withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationInterceptor.class);
            assertThat(context.getBean(AuthorizationInterceptor.class)).isSameAs(context.getBean("customAuthorizationInterceptor"));
        });
    }

    @Test
    void shouldRegisterRealInterceptorInWebMvcPipeline() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=REAL", "platform.authorization.service-url=http://localhost").run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
            configurer.addInterceptors(registry);
            assertThat(registry.getInterceptor()).isSameAs(context.getBean(AuthorizationInterceptor.class));
        });
    }

    @Test
    void shouldRegisterMockInterceptorInMockMode() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=MOCK").run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
            configurer.addInterceptors(registry);
            assertThat(registry.getInterceptor()).isSameAs(context.getBean(MockAuthorizationInterceptor.class));
        });
    }

    @Test
    void shouldPopulateGuestContextInMockMode() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=MOCK").run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
            configurer.addInterceptors(registry);
            HandlerInterceptor interceptor = registry.getInterceptor();
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(request.getHeader("User-Agent")).thenReturn("JUnit");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/api/test");
            boolean result = interceptor.preHandle(request, response, new Object());
            assertThat(result).isTrue();
            assertThat(UserContext.get()).isPresent();
            UserSession session = UserContext.get().orElseThrow();
            assertThat(session.getUserName()).isEqualTo("guest");
            assertThat(session.getEmail()).isEqualTo("guest@empresa.com");
            assertThat(session.getAccountId()).isEqualTo("account-guest");
            assertThat(session.getApplicationId()).isEqualTo("application-guest");
            assertThat(session.getEnvironmentId()).isEqualTo("environment-guest");
            assertThat(session.getTraceId()).isEqualTo("trace-guest");
            assertThat(session.getGroups()).containsExactly("GUEST");
            assertThat(session.getAuthorizerGroups()).containsExactly(
                    new ParsedGroup(
                            "GUEST",
                            "GUEST",
                            "environment-guest",
                            "GUEST"
                    )
            );
            assertThat(session.hasAuthorizer("GUEST")).isTrue();
            assertThat(org.slf4j.MDC.get("correlationId")).isEqualTo("trace-guest");
            assertThat(org.slf4j.MDC.get("username")).isEqualTo("guest");
            assertThat(org.slf4j.MDC.get("clientIp")).isEqualTo("127.0.0.1");
            assertThat(org.slf4j.MDC.get("userAgent")).isEqualTo("JUnit");
            assertThat(org.slf4j.MDC.get("uri")).isEqualTo("/api/test");
            assertThat(org.slf4j.MDC.get("accountId")).isEqualTo("account-guest");
            assertThat(org.slf4j.MDC.get("environmentId")).isEqualTo("environment-guest");
            assertThat(org.slf4j.MDC.get("applicationId")).isEqualTo("application-guest");
        });
    }

    @Test
    void shouldDeriveMockAuthorizerGroupsFromConfiguredGroupsInMockMode() {
        contextRunner
                .withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues(
                        "platform.authorization.mode=MOCK",
                        "platform.authorization.mock.user-name=bbs",
                        "platform.authorization.mock.email=bruno.barbosa@empresa.com.br",
                        "platform.authorization.mock.account-id=-",
                        "platform.authorization.mock.application-id=-",
                        "platform.authorization.mock.environment-id=-",
                        "platform.authorization.mock.trace-id=81f3503a-8153-4b80-9ee8-5ef41e611b31",
                        "platform.authorization.mock.groups[0]=PM5-ENG-DEV_WSE"
                )
                .run(context -> {
                    WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
                    CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
                    configurer.addInterceptors(registry);

                    HandlerInterceptor interceptor = registry.getInterceptor();
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
                    when(request.getRequestURI()).thenReturn("/api/test");

                    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

                    UserSession session = UserContext.get().orElseThrow();
                    assertThat(session.getGroups()).containsExactly("PM5-ENG-DEV_WSE");
                    assertThat(session.getAuthorizerGroups()).containsExactly(
                            new ParsedGroup("PM5-ENG-DEV_WSE", "ENG", "DEV", "WSE")
                    );
                    assertThat(session.hasAuthorizer("WSE")).isTrue();
                    assertThat(session.hasAuthorizer("GUEST")).isFalse();
                });
    }

    @Test
    void shouldPopulateConfiguredMockGroupsAndAuthorizerGroupsInMockMode() {
        contextRunner
                .withUserConfiguration(InfrastructureConfiguration.class)
                .withPropertyValues(
                        "platform.authorization.mode=MOCK",
                        "platform.authorization.mock.user-name=local-user",
                        "platform.authorization.mock.email=local-user@empresa.com",
                        "platform.authorization.mock.account-id=account-local",
                        "platform.authorization.mock.application-id=application-local",
                        "platform.authorization.mock.environment-id=DEV",
                        "platform.authorization.mock.trace-id=trace-local",
                        "platform.authorization.mock.groups[0]=USER",
                        "platform.authorization.mock.groups[1]=TESTER",
                        "platform.authorization.mock.authorizer-groups[0].full-group=GRP_WORKSPACE_DEV_TEAM_A",
                        "platform.authorization.mock.authorizer-groups[0].profile=DEV",
                        "platform.authorization.mock.authorizer-groups[0].environment=DEV",
                        "platform.authorization.mock.authorizer-groups[0].authorizer=TEAM_A",
                        "platform.authorization.mock.authorizer-groups[1].full-group=GRP_WORKSPACE_DEV_TEAM_B",
                        "platform.authorization.mock.authorizer-groups[1].profile=DEV",
                        "platform.authorization.mock.authorizer-groups[1].environment=DEV",
                        "platform.authorization.mock.authorizer-groups[1].authorizer=TEAM_B"
                )
                .run(context -> {
                    WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
                    CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
                    configurer.addInterceptors(registry);

                    HandlerInterceptor interceptor = registry.getInterceptor();
                    HttpServletRequest request = mock(HttpServletRequest.class);
                    HttpServletResponse response = mock(HttpServletResponse.class);
                    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
                    when(request.getRequestURI()).thenReturn("/api/test");

                    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

                    UserSession session = UserContext.get().orElseThrow();
                    assertThat(session.getUserName()).isEqualTo("local-user");
                    assertThat(session.getEmail()).isEqualTo("local-user@empresa.com");
                    assertThat(session.getAccountId()).isEqualTo("account-local");
                    assertThat(session.getApplicationId()).isEqualTo("application-local");
                    assertThat(session.getEnvironmentId()).isEqualTo("DEV");
                    assertThat(session.getTraceId()).isEqualTo("trace-local");
                    assertThat(session.getGroups()).containsExactlyInAnyOrder("USER", "TESTER");
                    assertThat(session.getAuthorizerGroups()).containsExactlyInAnyOrder(
                            new ParsedGroup("GRP_WORKSPACE_DEV_TEAM_A", "DEV", "DEV", "TEAM_A"),
                            new ParsedGroup("GRP_WORKSPACE_DEV_TEAM_B", "DEV", "DEV", "TEAM_B")
                    );
                    assertThat(session.hasAuthorizer("TEAM_A")).isTrue();
                    assertThat(session.hasAuthorizer("TEAM_B")).isTrue();
                    assertThat(session.hasAuthorizer("TEAM_C")).isFalse();
                });
    }

    @Test
    void shouldUseMockAgentWhenUserAgentIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=MOCK").run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
            configurer.addInterceptors(registry);
            HandlerInterceptor interceptor = registry.getInterceptor();
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(request.getHeader("User-Agent")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/api/test");
            boolean result = interceptor.preHandle(request, response, new Object());
            assertThat(result).isTrue();
            assertThat(org.slf4j.MDC.get("userAgent")).isEqualTo("mock-agent");
        });
    }

    @Test
    void shouldClearGuestContextAndMdcAfterCompletion() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.mode=MOCK").run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
            configurer.addInterceptors(registry);
            HandlerInterceptor interceptor = registry.getInterceptor();
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");
            when(request.getRequestURI()).thenReturn("/api/test");
            interceptor.preHandle(request, response, new Object());
            assertThat(UserContext.get()).isPresent();
            assertThat(org.slf4j.MDC.get("username")).isEqualTo("guest");
            assertThat(org.slf4j.MDC.get("correlationId")).isEqualTo("trace-guest");
            interceptor.afterCompletion(request, response, new Object(), null);
            assertThat(UserContext.get()).isEmpty();
            assertThat(org.slf4j.MDC.get("username")).isNull();
            assertThat(org.slf4j.MDC.get("correlationId")).isNull();
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
    static class CustomClientServiceConfiguration {
        @Bean
        AuthorizationClientService customAuthorizationClientService() {
            return mock(AuthorizationClientService.class);
        }
    }

    @Configuration
    static class CustomInterceptorConfiguration {
        @Bean
        AuthorizationInterceptor customAuthorizationInterceptor() {
            return mock(AuthorizationInterceptor.class);
        }
    }

    static class CapturingInterceptorRegistry extends InterceptorRegistry {

        private HandlerInterceptor interceptor;

        @Override
        public org.springframework.web.servlet.config.annotation.InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
            this.interceptor = interceptor;
            return super.addInterceptor(interceptor);
        }

        HandlerInterceptor getInterceptor() {
            assertThat(interceptor).as("O interceptor deveria ter sido registrado.").isNotNull();
            return interceptor;
        }
    }
}
