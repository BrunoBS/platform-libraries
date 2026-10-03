package br.com.portalmanager.platform.library.messagequeue.resolver;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DestinationResolverTest {

    @Test
    void shouldResolveAwsDefaultsOverridesAndDeadLetterReference() {
        var properties = new MessageQueueProperties();
        properties.setProvider(MessageQueueProvider.AWS);
        properties.getAws().setRegion("sa-east-1");
        properties.getAws().getDefaults().setVisibilityTimeout(Duration.ofSeconds(30));
        properties.getAws().getDefaults().setWaitTime(Duration.ofSeconds(20));
        properties.getAws().getDefaults().setConcurrency(5);

        var destination = new MessageQueueProperties.Destination();
        destination.setQueue("pm-product-updated");
        destination.getAws().setDeadLetterQueue("pm-product-updated-dlq");
        destination.getAws().setVisibilityTimeout(Duration.ofSeconds(120));
        destination.getAws().setConcurrency(10);
        properties.getDestinations().put("product-updated", destination);

        var resolved = new DestinationResolver(properties).resolve("product-updated");

        assertThat(resolved.provider()).isEqualTo(MessageQueueProvider.AWS);
        assertThat(resolved.queue()).isEqualTo("pm-product-updated");
        assertThat(resolved.deadLetterReference()).isEqualTo("pm-product-updated-dlq");
        assertThat(resolved.visibilityTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(resolved.waitTime()).isEqualTo(Duration.ofSeconds(20));
        assertThat(resolved.concurrency()).isEqualTo(10);
    }

    @Test
    void shouldResolveConventionalAwsDeadLetterQueueNameWhenNoOverrideIsSet() {
        var properties = new MessageQueueProperties();
        properties.setProvider(MessageQueueProvider.AWS);
        properties.getAws().setRegion("sa-east-1");

        var destination = new MessageQueueProperties.Destination();
        destination.setQueue("pm-product-updated");
        properties.getDestinations().put("product-updated", destination);

        var resolved = new DestinationResolver(properties).resolve("product-updated");

        assertThat(resolved.deadLetterReference()).isEqualTo("pm-product-updated-dlq");
    }

    @Test
    void shouldResolveFifoQueueAndConventionalFifoDeadLetterName() {
        var properties = new MessageQueueProperties();
        properties.setProvider(MessageQueueProvider.AWS);
        properties.getAws().setRegion("sa-east-1");

        var destination = new MessageQueueProperties.Destination();
        destination.setQueue("orders.fifo");
        destination.setOrdered(true);
        properties.getDestinations().put("orders", destination);

        var resolved = new DestinationResolver(properties).resolve("orders");

        assertThat(resolved.ordered()).isTrue();
        assertThat(resolved.deadLetterReference()).isEqualTo("orders-dlq.fifo");
    }

    @Test
    void shouldNotExposeIndependentDeadLetterReferenceForAzure() {
        var properties = new MessageQueueProperties();
        properties.setProvider(MessageQueueProvider.AZURE);
        properties.getAzure().setNamespace("pm.servicebus.windows.net");

        var destination = new MessageQueueProperties.Destination();
        destination.setQueue("pm-product-updated");
        properties.getDestinations().put("product-updated", destination);

        var resolved = new DestinationResolver(properties).resolve("product-updated");

        assertThat(resolved.provider()).isEqualTo(MessageQueueProvider.AZURE);
        assertThat(resolved.queue()).isEqualTo("pm-product-updated");
        assertThat(resolved.deadLetterReference()).isNull();
    }

    @Test
    void shouldFailWhenDestinationDoesNotExist() {
        var properties = new MessageQueueProperties();
        properties.setProvider(MessageQueueProvider.AWS);

        assertThatThrownBy(() -> new DestinationResolver(properties).resolve("missing"))
                .isInstanceOf(PlatformConfigurationException.class)
                .hasMessageContaining("missing");
    }
}
