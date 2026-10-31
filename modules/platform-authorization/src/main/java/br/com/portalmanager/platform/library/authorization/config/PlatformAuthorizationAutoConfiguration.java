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


@AutoConfiguration(afterName = "br.com.portalmanager.platform.library.messagequeue.provider.aws.configuration.AwsMessageQueueAutoConfiguration")
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
            br.com.portalmanager.platform.library.authorization.aop.EntryPolicyAuthorizer policyAuthorizer) {
        return new AuthorizationFacadeAspect(clientService, policyAuthorizer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "platform.authorization", name = "mode", havingValue = "MOCK")
    public MockAuthorizationFacadeAspect mockAuthorizationFacadeAspect(PlatformAuthorizationProperties properties,
            br.com.portalmanager.platform.library.authorization.aop.EntryPolicyAuthorizer policyAuthorizer) {
        return new MockAuthorizationFacadeAspect(properties, policyAuthorizer);
    }

    @Bean
    public static br.com.portalmanager.platform.library.authorization.config.EntryPolicyConfigurationValidator entryPolicyConfigurationValidator(
            PlatformAuthorizationProperties properties) {
        return new br.com.portalmanager.platform.library.authorization.config.EntryPolicyConfigurationValidator(properties);
    }

    @Bean
    public br.com.portalmanager.platform.library.authorization.aop.EntryPolicyAuthorizer entryPolicyAuthorizer(
            PlatformAuthorizationProperties properties,
            org.springframework.beans.factory.ObjectProvider<br.com.portalmanager.platform.library.authorization.model.AuthorizationEntryOriginResolver> origins) {
        return new br.com.portalmanager.platform.library.authorization.aop.EntryPolicyAuthorizer(properties, origins.orderedStream().toList());
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer")
    static class SqsPolicyConfiguration {
        @Bean
        @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(type = "br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer")
        br.com.portalmanager.platform.library.authorization.model.AuthorizationEntryOriginResolver sqsAuthorizationEntryOriginResolver() {
            return () -> br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer.currentDelivery()
                    .map(value -> new br.com.portalmanager.platform.library.authorization.model.AuthorizationEntryOrigin(
                        value.entryPoint(), value.destination(), value.queueUrl(), value.messageId(), value.correlationId(), value.timestamp()));
        }

        @Bean
        SmartInitializingSingleton sqsEntryPolicyDestinationValidator(PlatformAuthorizationProperties properties,
                org.springframework.beans.factory.ObjectProvider<br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry> registries,
                org.springframework.beans.factory.ObjectProvider<br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver> destinations) {
            return () -> {
                if (properties.getEntryPolicies().isEmpty()) return;
                if (registries.stream().count() != 1 || destinations.stream().count() != 1) throw invalid();
                for (br.com.portalmanager.platform.library.authorization.model.AuthorizationEntryPolicy policy : properties.getEntryPolicies().values()) {
                    if (policy == null) throw invalid();
                    long matches = registries.getObject().listeners().stream().filter(listener ->
                        listener.destination().equals(policy.destination())
                        && (org.springframework.aop.support.AopUtils.getTargetClass(listener.bean()).getName() + "#" + listener.method().getName()).equals(policy.entryPoint())).count();
                    if (matches != 1) throw invalid();
                    br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination destination = destinations.getObject().resolve(policy.destination());
                    java.net.URI uri;
                    try { uri = java.net.URI.create(policy.queueUrl()); } catch (RuntimeException failure) { throw invalid(); }
                    if (!destination.consumerEnabled() || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getQuery() != null
                            || uri.getFragment() != null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                            || !uri.getPath().endsWith("/" + destination.queue())) throw invalid();
                }
            };
        }
        private static PlatformConfigurationException invalid() {
            return new PlatformConfigurationException(AuthorizationTechnicalErrors.ENTRY_POLICY_INVALID);
        }
    }

}
