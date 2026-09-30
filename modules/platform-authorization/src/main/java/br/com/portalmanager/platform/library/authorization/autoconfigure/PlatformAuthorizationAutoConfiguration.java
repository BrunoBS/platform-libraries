package br.com.portalmanager.platform.library.authorization.autoconfigure;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.authorization.model.AuthorizerGroupParser;
import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.registry.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.authorization.service.AuthorizationClientService;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationInterceptor;
import br.com.portalmanager.platform.library.authorization.web.filter.AuthorizationContextCleanupFilter;
import br.com.portalmanager.platform.library.authorization.web.filter.PayloadErrorLoggingFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationMetadataRegistry authorizationMetadataRegistry() {
        return new AuthorizationMetadataRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationContextCleanupFilter authorizationContextCleanupFilter() {
        return new AuthorizationContextCleanupFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuthorizationClientService authorizationClientService(
            RestClient.Builder builder,
            PlatformAuthorizationProperties properties) {

        String authUrl = properties.getServiceUrl();
        if (authUrl == null || authUrl.isBlank()) {
            throw new PlatformConfigurationException(
                    AuthorizationTechnicalErrors.SERVICE_URL_REQUIRED
            );
        }
        return new AuthorizationClientService(builder, authUrl);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AuthorizationInterceptor authorizationInterceptor(
            AuthorizationClientService clientService,
            AuthorizationMetadataRegistry metadataRegistry) {
        return new AuthorizationInterceptor(clientService, metadataRegistry);
    }

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

    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "false")
    public WebMvcConfigurer mockInterceptorConfigurer(PlatformAuthorizationProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        PlatformAuthorizationProperties.Mock mock = properties.getMock();

                        UserSession mockSession = new UserSession();
                        mockSession.setUserName(mock.getUserName());
                        mockSession.setEmail(mock.getEmail());
                        mockSession.setAccountId(mock.getAccountId());
                        mockSession.setApplicationId(mock.getApplicationId());
                        mockSession.setEnvironmentId(mock.getEnvironmentId());
                        mockSession.setTraceId(mock.getTraceId());
                        mockSession.setExpirationTime(Instant.now().plusSeconds(3600).toEpochMilli());
                        mockSession.setGroups(mock.getGroups());

                        Set<ParsedGroup> authorizerGroups;
                        if (!mock.getAuthorizerGroups().isEmpty()) {
                            authorizerGroups = Set.copyOf(mock.getAuthorizerGroups().stream()
                                    .map(group -> new ParsedGroup(
                                            group.getFullGroup(),
                                            group.getProfile(),
                                            group.getEnvironment(),
                                            group.getAuthorizer()))
                                    .toList());
                        } else {
                            authorizerGroups = AuthorizerGroupParser.parseAll(mock.getGroups());
                            if (authorizerGroups.isEmpty()) {
                                authorizerGroups = Set.of(
                                        new ParsedGroup("GUEST", "GUEST", mock.getEnvironmentId(), "GUEST")
                                );
                            }
                        }
                        mockSession.setAuthorizerGroups(authorizerGroups);
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

    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
    public PayloadErrorLoggingFilter payloadErrorLoggingFilter() {
        return new PayloadErrorLoggingFilter();
    }
}
