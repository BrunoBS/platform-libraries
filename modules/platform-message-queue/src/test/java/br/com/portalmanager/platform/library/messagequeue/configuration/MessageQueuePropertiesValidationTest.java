package br.com.portalmanager.platform.library.messagequeue.configuration;

import br.com.portalmanager.platform.library.messagequeue.exception.MessageQueueConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MessageQueuePropertiesValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void shouldStartWhenAwsConfigurationIsComplete() {
        contextRunner
                .withPropertyValues(
                        "platform.message-queue.provider=AWS",
                        "platform.message-queue.aws.region=sa-east-1",
                        "platform.message-queue.destinations.orders.queue=orders")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldStartWhenAzureConfigurationIsComplete() {
        contextRunner
                .withPropertyValues(
                        "platform.message-queue.provider=AZURE",
                        "platform.message-queue.azure.namespace=orders.servicebus.windows.net",
                        "platform.message-queue.destinations.orders.queue=orders")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldFailAtStartupWhenProviderIsMissing() {
        contextRunner
                .withPropertyValues("platform.message-queue.destinations.orders.queue=orders")
                .run(context -> assertInvalid(context.getStartupFailure(), "provider is required"));
    }

    @Test
    void shouldFailAtStartupWhenAwsRegionIsMissing() {
        contextRunner
                .withPropertyValues(
                        "platform.message-queue.provider=AWS",
                        "platform.message-queue.destinations.orders.queue=orders")
                .run(context -> assertInvalid(context.getStartupFailure(), "aws.region is required"));
    }

    @Test
    void shouldFailAtStartupWhenAzureNamespaceIsMissing() {
        contextRunner
                .withPropertyValues(
                        "platform.message-queue.provider=AZURE",
                        "platform.message-queue.destinations.orders.queue=orders")
                .run(context -> assertInvalid(context.getStartupFailure(), "azure.namespace is required"));
    }

    @Test
    void shouldFailAtStartupWhenConsumerSettingsAreOutOfRange() {
        contextRunner
                .withPropertyValues(
                        "platform.message-queue.provider=AWS",
                        "platform.message-queue.aws.region=sa-east-1",
                        "platform.message-queue.destinations.orders.queue=orders",
                        "platform.message-queue.destinations.orders.aws.wait-time=21s")
                .run(context -> assertInvalid(context.getStartupFailure(), "wait-time"));
    }

    @Test
    void shouldFailAtStartupWhenUnusedProviderToggleIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "platform.message-queue.provider=AWS",
                        "platform.message-queue.aws.region=sa-east-1",
                        "platform.message-queue.aws.defaults.enabled=false",
                        "platform.message-queue.destinations.orders.queue=orders")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("enabled");
                });
    }

    @Test
    void shouldFailAtStartupWhenConcurrencyIsNotPositive() {
        contextRunner
                .withPropertyValues(
                        "platform.message-queue.provider=AWS",
                        "platform.message-queue.aws.region=sa-east-1",
                        "platform.message-queue.destinations.orders.queue=orders",
                        "platform.message-queue.destinations.orders.consumer.concurrency=0")
                .run(context -> assertInvalid(context.getStartupFailure(), "concurrency"));
    }

    private void assertInvalid(Throwable failure, String message) {
        assertThat(failure).isNotNull();
        assertThat(failure).hasRootCauseInstanceOf(MessageQueueConfigurationException.class);
        assertThat(failure).hasStackTraceContaining(message);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MessageQueueProperties.class)
    static class PropertiesConfiguration {
    }
}
