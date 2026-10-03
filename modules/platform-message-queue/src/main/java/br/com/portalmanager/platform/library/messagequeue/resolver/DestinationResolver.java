package br.com.portalmanager.platform.library.messagequeue.resolver;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageQueueConfigurationException;

import java.time.Duration;

public class DestinationResolver {

    private final MessageQueueProperties properties;

    public DestinationResolver(MessageQueueProperties properties) {
        this.properties = properties;
    }

    public ResolvedDestination resolve(String destination) {
        if (properties.getProvider() == null) {
            throw new MessageQueueConfigurationException("Message queue provider is required");
        }

        var configured = properties.getDestinations().get(destination);
        if (configured == null) {
            throw new MessageQueueConfigurationException("Message queue destination not found: " + destination);
        }
        if (configured.getQueue() == null || configured.getQueue().isBlank()) {
            throw new MessageQueueConfigurationException("Physical queue is required for destination: " + destination);
        }

        var providerDefaults = providerDefaults(properties.getProvider());
        var providerOverride = providerOverride(properties.getProvider(), configured);

        return new ResolvedDestination(
                destination,
                properties.getProvider(),
                configured.getQueue(),
                configured.getDeadLetterQueue(),
                configured.getPublisher().isEnabled(),
                configured.getConsumer().isEnabled(),
                firstNonNull(providerOverride.getVisibilityTimeout(), configured.getConsumer().getVisibilityTimeout(), providerDefaults.getVisibilityTimeout()),
                firstNonNull(providerOverride.getWaitTime(), configured.getConsumer().getWaitTime(), providerDefaults.getWaitTime()),
                firstNonNull(providerOverride.getConcurrency(), configured.getConsumer().getConcurrency(), providerDefaults.getConcurrency())
        );
    }

    private MessageQueueProperties.Consumer providerDefaults(MessageQueueProvider provider) {
        return provider == MessageQueueProvider.AWS
                ? properties.getAws().getDefaults()
                : properties.getAzure().getDefaults();
    }

    private MessageQueueProperties.Consumer providerOverride(
            MessageQueueProvider provider,
            MessageQueueProperties.Destination destination) {
        return provider == MessageQueueProvider.AWS ? destination.getAws() : destination.getAzure();
    }

    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
