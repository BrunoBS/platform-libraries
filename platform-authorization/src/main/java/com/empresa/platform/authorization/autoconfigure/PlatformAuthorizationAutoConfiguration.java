package com.empresa.platform.authorization.autoconfigure;

import com.empresa.platform.authorization.config.PlatformAuthorizationProperties;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;
import com.empresa.platform.authorization.registry.AuthorizationMetadataRegistry;
import com.empresa.platform.authorization.service.AuthorizationClientService;
import com.empresa.platform.authorization.web.AuthorizationInterceptor;
import com.empresa.platform.messaging.config.PlatformMessagingProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.util.Set;

@AutoConfiguration
@EnableConfigurationProperties(PlatformAuthorizationProperties.class)
public class PlatformAuthorizationAutoConfiguration {

    // =========================================================================
    // Componentes Compartilhados que nascem no Boot
    // =========================================================================

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationMetadataRegistry authorizationMetadataRegistry(ApplicationContext context) {
        return new AuthorizationMetadataRegistry(context);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuthorizationClientService authorizationClientService(
            RestClient.Builder builder,
            PlatformAuthorizationProperties properties) {

        String authUrl = properties.getServiceUrl();
        if (authUrl == null || authUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "A propriedade [platform.authorization.service-url] e obrigatoria quando o modulo de autorizacao esta ativo."
            );
        }
        return new AuthorizationClientService(builder, authUrl);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuthorizationInterceptor authorizationInterceptor(
            AuthorizationClientService clientService,
            AuthorizationMetadataRegistry metadataRegistry,
            PlatformMessagingProperties messagingProperties) {
        return new AuthorizationInterceptor(clientService, metadataRegistry, messagingProperties);
    }

    // =========================================================================
    // Registro Condicional do Pipeline Web MVC (Sem classes internas para evitar ciclos)
    // =========================================================================

    /**
     * CENÁRIO REAL: Registra o interceptor de segurança real se habilitado
     */
    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer realInterceptorConfigurer(AuthorizationInterceptor realInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(realInterceptor).addPathPatterns("/**");
            }
        };
    }

    /**
     * CENÁRIO MOCK GUEST: Registra o interceptor fake injetando perfil guest se desabilitado
     */
    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "false")
    public WebMvcConfigurer mockInterceptorConfigurer(PlatformMessagingProperties messagingProperties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        UserSession mockSession = new UserSession();
                        mockSession.setUserName("guest");
                        mockSession.setEmail("guest@empresa.com");
                        mockSession.setAccountId("account-guest");
                        mockSession.setApplicationId("application-guest");
                        mockSession.setEnvironmentId("environment-guest");
                        mockSession.setTraceId("trace-guest");
                        mockSession.setExpirationTime(Instant.now().plusSeconds(3600).toEpochMilli());
                        mockSession.setGroups(Set.of("GUEST"));

                        UserContext.set(mockSession);

                        String userAgent = request.getHeader("User-Agent");


                        MDC.put("correlationId", mockSession.getTraceId());
                        MDC.put("username", mockSession.getUserName());
                        MDC.put("clientIp", request.getRemoteAddr());
                        MDC.put("userAgent", userAgent != null ? userAgent : "mock-agent");
                        MDC.put("uri", request.getRequestURI());
                        MDC.put("accountId", mockSession.getAccountId());
                        MDC.put("environmentId", mockSession.getEnvironmentId());
                        MDC.put("applicationId", mockSession.getApplicationId());

                        return true;
                    }

                    @Override
                    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                        UserContext.clear();
                        MDC.clear();
                    }
                }).addPathPatterns("/**");
            }
        };
    }
}
