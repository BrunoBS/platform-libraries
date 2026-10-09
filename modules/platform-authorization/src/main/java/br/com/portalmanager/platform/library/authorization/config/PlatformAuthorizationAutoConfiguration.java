package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationTechnicalErrors;
import org.springframework.beans.factory.SmartInitializingSingleton;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.aop.AuthorizationFacadeAspect;
import br.com.portalmanager.platform.library.authorization.aop.MockAuthorizationFacadeAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.Set;
import org.springframework.core.env.Environment;


@AutoConfiguration
@EnableConfigurationProperties(PlatformAuthorizationProperties.class)
public class PlatformAuthorizationAutoConfiguration {

    @Bean
    public SmartInitializingSingleton authorizationContextPropagationConfigurationValidator(
            PlatformAuthorizationProperties properties, Environment environment) {
        return () -> {
            if (properties.getMode() == AuthorizationMode.MOCK) {
                Set<String> permitted = Set.of("local", "test");
                String[] active = environment.getActiveProfiles();
                if (active.length == 0 || Arrays.stream(active).anyMatch(profile -> !permitted.contains(profile))) {
                    throw new PlatformConfigurationException(AuthorizationTechnicalErrors.MOCK_MODE_FORBIDDEN);
                }
            }
            var settings = properties.getContextPropagation();
            if (settings.isDefaultExecutor() && !settings.isEnabled()) {
                throw new PlatformConfigurationException(
                        AuthorizationTechnicalErrors.CONTEXT_PROPAGATION_REQUIRED);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationMetadataRegistry authorizationMetadataRegistry() {
        return new AuthorizationMetadataRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "REAL", matchIfMissing = true)
    public AuthorizationClient authorizationClient(
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

        return new AuthorizationClient(
                authorizationBuilder,
                authUrl,
                properties.getRetry()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "REAL", matchIfMissing = true)
    public AuthorizationFacadeAspect authorizationFacadeAspect(AuthorizationClient clientService) {
        return new AuthorizationFacadeAspect(clientService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "MOCK")
    public MockAuthorizationFacadeAspect mockAuthorizationFacadeAspect(PlatformAuthorizationProperties properties) {
        return new MockAuthorizationFacadeAspect(properties);
    }

}
