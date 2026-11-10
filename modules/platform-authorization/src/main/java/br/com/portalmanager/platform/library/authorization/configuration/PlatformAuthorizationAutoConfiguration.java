package br.com.portalmanager.platform.library.authorization.configuration;

import br.com.portalmanager.platform.library.authorization.api.configuration.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.api.configuration.AuthorizationExecutionMode;
import br.com.portalmanager.platform.library.authorization.internal.message.AuthorizationTechnicalErrors;
import org.springframework.beans.factory.SmartInitializingSingleton;
import br.com.portalmanager.platform.library.messaging.api.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.authorization.api.configuration.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.authorization.api.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.internal.aop.AuthorizationFacadeAspect;
import br.com.portalmanager.platform.library.authorization.internal.aop.MockAuthorizationFacadeAspect;
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


@AutoConfiguration(afterName = {
        "br.com.portalmanager.platform.library.messagequeue.provider.aws.configuration.AwsMessageQueueAutoConfiguration",
        "br.com.portalmanager.platform.library.messagequeue.provider.azure.configuration.AzureMessageQueueAutoConfiguration"
})
@EnableConfigurationProperties(PlatformAuthorizationProperties.class)
public class PlatformAuthorizationAutoConfiguration {

    @Bean
    public SmartInitializingSingleton authorizationContextPropagationConfigurationValidator(
            PlatformAuthorizationProperties properties, Environment environment) {
        return () -> {
            String configuredMode = environment.getProperty("platform.authorization.mode");
            if (configuredMode != null
                    && !properties.getMode().name().equalsIgnoreCase(configuredMode)) {
                throw new PlatformConfigurationException(AuthorizationTechnicalErrors.MODE_INVALID);
            }
            if (properties.getMode() == AuthorizationExecutionMode.MOCK) {
                Set<String> permitted = Set.of("local", "test", "dev");
                String[] active = environment.getActiveProfiles();
                if (active.length == 0 || Arrays.stream(active).anyMatch(profile -> !permitted.contains(profile))) {
                    throw new PlatformConfigurationException(AuthorizationTechnicalErrors.MOCK_MODE_FORBIDDEN);
                }
            }
            PlatformAuthorizationProperties.ContextPropagation settings = properties.getContextPropagation();
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
    public AuthorizationFacadeAspect authorizationFacadeAspect(AuthorizationClient clientService,
            br.com.portalmanager.platform.library.authorization.internal.aop.EntryPolicyAuthorizer policyAuthorizer) {
        return new AuthorizationFacadeAspect(clientService, policyAuthorizer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "MOCK")
    public MockAuthorizationFacadeAspect mockAuthorizationFacadeAspect(PlatformAuthorizationProperties properties,
            br.com.portalmanager.platform.library.authorization.internal.aop.EntryPolicyAuthorizer policyAuthorizer) {
        return new MockAuthorizationFacadeAspect(properties, policyAuthorizer);
    }

    @Bean
    public static br.com.portalmanager.platform.library.authorization.internal.configuration.EntryPolicyConfigurationValidator entryPolicyConfigurationValidator(
            PlatformAuthorizationProperties properties) {
        return new br.com.portalmanager.platform.library.authorization.internal.configuration.EntryPolicyConfigurationValidator(properties);
    }

    @Bean
    public br.com.portalmanager.platform.library.authorization.internal.aop.EntryPolicyAuthorizer entryPolicyAuthorizer(
            PlatformAuthorizationProperties properties,
            org.springframework.beans.factory.ObjectProvider<br.com.portalmanager.platform.library.authorization.api.model.AuthorizationEntryOriginResolver> origins) {
        return new br.com.portalmanager.platform.library.authorization.internal.aop.EntryPolicyAuthorizer(properties, origins.orderedStream().toList());
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueDeliveryContext")
    static class MessageQueuePolicyConfiguration {
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(type = "br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueListenerCatalog")
        br.com.portalmanager.platform.library.authorization.api.model.AuthorizationEntryOriginResolver messageQueueAuthorizationEntryOriginResolver() {
            return () -> br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueDeliveryContext.current()
                    .map(value -> new br.com.portalmanager.platform.library.authorization.api.model.AuthorizationEntryOrigin(
                            value.entryPoint(), value.destination(), value.resourceIdentifier(), value.messageId(),
                            value.correlationId(), value.timestamp()));
        }

        @Bean
        SmartInitializingSingleton messageQueueEntryPolicyDestinationValidator(PlatformAuthorizationProperties properties,
                org.springframework.beans.factory.ObjectProvider<br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueListenerCatalog> registries,
                org.springframework.beans.factory.ObjectProvider<br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueDestinationCatalog> destinations) {
            return () -> {
                if (properties.getEntryPolicies().isEmpty()) return;
                if (registries.stream().count() != 1 || destinations.stream().count() != 1) throw invalidEntryPolicy();
                var registry = registries.getObject();
                var destinationCatalog = destinations.getObject();
                for (var policy : properties.getEntryPolicies().values()) {
                    if (policy == null) throw invalidEntryPolicy();
                    long matches = registry.registrations().stream().filter(listener ->
                            listener.destination().equals(policy.destination())
                                    && listener.entryPoint().equals(policy.entryPoint())).count();
                    if (matches != 1) throw invalidEntryPolicy();
                    var destination = destinationCatalog.resolveForIntegration(policy.destination());
                    if (!destination.consumerEnabled() || !matchesResource(policy.queueUrl(), destination.resourceIdentifier())) {
                        throw invalidEntryPolicy();
                    }
                }
            };
        }

        private static boolean matchesResource(String configured, String resolved) {
            if (configured == null || resolved == null) return false;
            if (configured.equals(resolved)) return true;
            try {
                java.net.URI uri = java.net.URI.create(configured);
                return uri.getHost() != null && uri.getRawUserInfo() == null && uri.getQuery() == null
                        && uri.getFragment() == null && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                        && uri.getPath().endsWith("/" + resolved);
            } catch (RuntimeException failure) { return false; }
        }

        private static PlatformConfigurationException invalidEntryPolicy() {
            return new PlatformConfigurationException(AuthorizationTechnicalErrors.ENTRY_POLICY_INVALID);
        }
    }

}
