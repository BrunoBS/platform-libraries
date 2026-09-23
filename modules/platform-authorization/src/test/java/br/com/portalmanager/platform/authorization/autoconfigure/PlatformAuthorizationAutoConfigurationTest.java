package br.com.portalmanager.platform.authorization.autoconfigure;

import br.com.portalmanager.platform.authorization.model.UserContext;
import br.com.portalmanager.platform.authorization.model.UserSession;
import br.com.portalmanager.platform.authorization.registry.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.authorization.service.AuthorizationClientService;
import br.com.portalmanager.platform.authorization.web.AuthorizationInterceptor;
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
    void shouldLoadRealSecurityBeansWhenEnabled() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=true", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
            assertThat(context).hasSingleBean(AuthorizationClientService.class);
            assertThat(context).hasSingleBean(AuthorizationInterceptor.class);
            assertThat(context).hasSingleBean(WebMvcConfigurer.class);
            assertThat(context).doesNotHaveBean("mockInterceptorConfigurer");
        });
    }

    @Test
    void shouldUseEnabledByDefaultWhenPropertyIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationClientService.class);
            assertThat(context).hasSingleBean(AuthorizationInterceptor.class);
            assertThat(context).hasSingleBean(WebMvcConfigurer.class);
        });
    }

    @Test
    void shouldFailWhenServiceUrlIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
            assertThat(context.getStartupFailure()).hasRootCauseMessage("A propriedade [platform.authorization.service-url] e obrigatoria quando o modulo de autorizacao esta ativo.");
        });
    }

    @Test
    void shouldFailWhenServiceUrlIsBlank() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=true", "platform.authorization.service-url=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void shouldLoadMockGuestInterceptorWhenDisabled() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(AuthorizationClientService.class);
            assertThat(context).doesNotHaveBean(AuthorizationInterceptor.class);
            assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
            assertThat(context).hasSingleBean(WebMvcConfigurer.class);
        });
    }

    @Test
    void shouldNotCreateRealSecurityBeansWhenDisabled() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(AuthorizationClientService.class);
            assertThat(context).doesNotHaveBean(AuthorizationInterceptor.class);
        });
    }

    @Test
    void shouldKeepUserAuthorizationMetadataRegistry() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomRegistryConfiguration.class).withPropertyValues("platform.authorization.enabled=true", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationMetadataRegistry.class);
            assertThat(context.getBean(AuthorizationMetadataRegistry.class)).isSameAs(context.getBean("customAuthorizationMetadataRegistry"));
        });
    }

    @Test
    void shouldKeepUserAuthorizationClientService() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomClientServiceConfiguration.class).withPropertyValues("platform.authorization.enabled=true", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationClientService.class);
            assertThat(context.getBean(AuthorizationClientService.class)).isSameAs(context.getBean("customAuthorizationClientService"));
        });
    }

    @Test
    void shouldKeepUserAuthorizationInterceptor() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class, CustomInterceptorConfiguration.class).withPropertyValues("platform.authorization.enabled=true", "platform.authorization.service-url=http://localhost").run(context -> {
            assertThat(context).hasSingleBean(AuthorizationInterceptor.class);
            assertThat(context.getBean(AuthorizationInterceptor.class)).isSameAs(context.getBean("customAuthorizationInterceptor"));
        });
    }

    @Test
    void shouldRegisterRealInterceptorInWebMvcPipeline() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=true", "platform.authorization.service-url=http://localhost").run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
            configurer.addInterceptors(registry);
            assertThat(registry.getInterceptor()).isSameAs(context.getBean(AuthorizationInterceptor.class));
        });
    }

    @Test
    void shouldRegisterMockInterceptorWhenDisabled() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=false").run(context -> {
            WebMvcConfigurer configurer = context.getBean(WebMvcConfigurer.class);
            CapturingInterceptorRegistry registry = new CapturingInterceptorRegistry();
            configurer.addInterceptors(registry);
            assertThat(registry.getInterceptor()).isNotNull();
            assertThat(registry.getInterceptor()).isNotInstanceOf(AuthorizationInterceptor.class);
        });
    }

    @Test
    void shouldPopulateGuestContextWhenAuthorizationIsDisabled() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=false").run(context -> {
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
                    new br.com.portalmanager.platform.authorization.model.ParsedGroup(
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
    void shouldUseMockAgentWhenUserAgentIsMissing() {
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=false").run(context -> {
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
        contextRunner.withUserConfiguration(InfrastructureConfiguration.class).withPropertyValues("platform.authorization.enabled=false").run(context -> {
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
