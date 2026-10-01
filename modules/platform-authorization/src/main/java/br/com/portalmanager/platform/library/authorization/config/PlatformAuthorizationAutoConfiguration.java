package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationClientService;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationInterceptor;
import br.com.portalmanager.platform.library.authorization.web.MockAuthorizationInterceptor;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationContextCleanupFilter;
import br.com.portalmanager.platform.library.authorization.web.PayloadErrorLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


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
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "REAL", matchIfMissing = true)
    public AuthorizationClientService authorizationClientService(
            RestClient.Builder builder,
            PlatformAuthorizationProperties properties) {

        String authUrl = properties.getServiceUrl();
        if (authUrl == null || authUrl.isBlank()) {
            throw new PlatformConfigurationException(
                    AuthorizationTechnicalErrors.SERVICE_URL_REQUIRED
            );
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient.Builder authorizationBuilder = builder.clone()
                .requestFactory(requestFactory);

        return new AuthorizationClientService(
                authorizationBuilder,
                authUrl,
                properties.getRetry()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "REAL", matchIfMissing = true)
    public AuthorizationInterceptor authorizationInterceptor(
            AuthorizationClientService clientService,
            AuthorizationMetadataRegistry metadataRegistry) {
        return new AuthorizationInterceptor(clientService, metadataRegistry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "REAL", matchIfMissing = true)
    public WebMvcConfigurer realInterceptorConfigurer(AuthorizationInterceptor realInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(realInterceptor).addPathPatterns("/**");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "MOCK")
    public MockAuthorizationInterceptor mockAuthorizationInterceptor(PlatformAuthorizationProperties properties) {
        return new MockAuthorizationInterceptor(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "MOCK")
    public WebMvcConfigurer mockInterceptorConfigurer(MockAuthorizationInterceptor mockInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(mockInterceptor).addPathPatterns("/**");
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "REAL", matchIfMissing = true)
    public PayloadErrorLoggingFilter payloadErrorLoggingFilter() {
        return new PayloadErrorLoggingFilter();
    }
}
