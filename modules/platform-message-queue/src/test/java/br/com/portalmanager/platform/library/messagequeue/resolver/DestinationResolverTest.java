package br.com.portalmanager.platform.library.messagequeue.resolver;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageQueueConfigurationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DestinationResolverTest {

    @Test
    void shouldResolveProviderDefaultsAndDestinationOverride() {
        var properties = new MessageQueueProperties();
        properties.setProvider(MessageQueueProvider.AWS);
        properties.getAws().setRegion("sa-east-1");
        properties.getAws().getDefaults().setVisibilityTimeout(Duration.ofSeconds(30));
        properties.getAws().getDefaults().setWaitTime(Duration.ofSeconds(20));
        properties.getAws().getDefaults().setConcurrency(5);

        var destination = new MessageQueueProperties.Destination();
        destination.setQueue("pm-product-updated");
        destination.setDeadLetterQueue("pm-product-updated-dlq");
        destination.getAws().setVisibilityTimeout(Duration.ofSeconds(120));
        destination.getAws().setConcurrency(10);
        properties.getDestinations().put("product-updated", destination);

        var resolved = new DestinationResolver(properties).resolve("product-updated");

        assertThat(resolved.provider()).isEqualTo(MessageQueueProvider.AWS);
        assertThat(resolved.queue()).isEqualTo("pm-product-updated");
        assertThat(resolved.deadLetterQueue()).isEqualTo("pm-product-updated-dlq");
        assertThat(resolved.visibilityTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(resolved.waitTime()).isEqualTo(Duration.ofSeconds(20));
        assertThat(resolved.concurrency()).isEqualTo(10);
    }

    @Test
    void shouldFailWhenDestinationDoesNotExist() {
        var properties = new MessageQueueProperties();
        properties.setProvider(MessageQueueProvider.AWS);

        assertThatThrownBy(() -> new DestinationResolver(properties).resolve("missing"))
                .isInstanceOf(MessageQueueConfigurationException.class)
                .hasMessageContaining("missing");
    }
}
