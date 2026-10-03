package br.com.portalmanager.platform.library.messagequeue.resolver;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageQueueConfigurationException;

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
                resolveDeadLetterReference(properties.getProvider(), configured),
                configured.getPublisher().isEnabled(),
                configured.getConsumer().isEnabled(),
                resolveVisibilityTimeout(properties.getProvider(), configured),
                firstNonNull(providerOverride.getWaitTime(), configured.getConsumer().getWaitTime(), providerDefaults.getWaitTime()),
                firstNonNull(providerOverride.getConcurrency(), configured.getConsumer().getConcurrency(), providerDefaults.getConcurrency())
        );
    }

    private java.time.Duration resolveVisibilityTimeout(
            MessageQueueProvider provider,
            MessageQueueProperties.Destination destination) {
        if (provider != MessageQueueProvider.AWS) {
            return null;
        }
        return firstNonNull(
                destination.getAws().getVisibilityTimeout(),
                properties.getAws().getDefaults().getVisibilityTimeout());
    }

    private String resolveDeadLetterReference(
            MessageQueueProvider provider,
            MessageQueueProperties.Destination destination) {
        if (provider != MessageQueueProvider.AWS) {
            return null;
        }
        String configuredQueue = destination.getAws().getDeadLetterQueue();
        return configuredQueue == null || configuredQueue.isBlank()
                ? destination.getQueue() + "-dlq"
                : configuredQueue;
    }

    private MessageQueueProperties.ConsumerOptions providerDefaults(MessageQueueProvider provider) {
        return provider == MessageQueueProvider.AWS
                ? properties.getAws().getDefaults()
                : properties.getAzure().getDefaults();
    }

    private MessageQueueProperties.ConsumerOptions providerOverride(
            MessageQueueProvider provider,
            MessageQueueProperties.Destination destination) {
        return provider == MessageQueueProvider.AWS ? destination.getAws() : destination.getAzure();
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
