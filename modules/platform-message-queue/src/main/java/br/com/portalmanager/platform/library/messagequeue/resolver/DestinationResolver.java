package br.com.portalmanager.platform.library.messagequeue.resolver;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueTechnicalErrors;

public class DestinationResolver {

    private final MessageQueueProperties properties;

    public DestinationResolver(MessageQueueProperties properties) {
        this.properties = properties;
    }

    public ResolvedDestination resolve(String destination) {
        if (properties.getProvider() == null) {
            throw configurationError("Message queue provider is required");
        }

        var configured = properties.getDestinations().get(destination);
        if (configured == null) {
            throw configurationError("Message queue destination not found: " + destination);
        }
        if (configured.getQueue() == null || configured.getQueue().isBlank()) {
            throw configurationError("Physical queue is required for destination: " + destination);
        }

        var providerDefaults = providerDefaults(properties.getProvider());
        var providerOverride = providerOverride(properties.getProvider(), configured);
        boolean fifo = properties.getProvider() == MessageQueueProvider.AWS && configured.isOrdered();

        return new ResolvedDestination(
                destination,
                properties.getProvider(),
                configured.getQueue(),
                resolveDeadLetterReference(properties.getProvider(), configured, fifo),
                configured.getPublisher().isEnabled(),
                configured.getConsumer().isEnabled(),
                configured.isOrdered(),
                resolveVisibilityTimeout(properties.getProvider(), configured),
                firstNonNull(providerOverride.getWaitTime(), configured.getConsumer().getWaitTime(), providerDefaults.getWaitTime()),
                firstNonNull(providerOverride.getConcurrency(), configured.getConsumer().getConcurrency(), providerDefaults.getConcurrency())
        );
    }

    private java.time.Duration resolveVisibilityTimeout(
            MessageQueueProvider provider,
            MessageQueueProperties.Destination destination) {
        if (provider != MessageQueueProvider.AWS) return null;
        return firstNonNull(destination.getAws().getVisibilityTimeout(),
                properties.getAws().getDefaults().getVisibilityTimeout());
    }

    private String resolveDeadLetterReference(
            MessageQueueProvider provider,
            MessageQueueProperties.Destination destination,
            boolean fifo) {
        if (provider != MessageQueueProvider.AWS) return null;
        String configuredQueue = destination.getAws().getDeadLetterQueue();
        if (configuredQueue != null && !configuredQueue.isBlank()) return configuredQueue;
        String queue = destination.getQueue();
        return fifo ? queue.substring(0, queue.length() - ".fifo".length()) + "-dlq.fifo" : queue + "-dlq";
    }

    private MessageQueueProperties.ConsumerOptions providerDefaults(MessageQueueProvider provider) {
        return provider == MessageQueueProvider.AWS ? properties.getAws().getDefaults() : properties.getAzure().getDefaults();
    }

    private MessageQueueProperties.ConsumerOptions providerOverride(
            MessageQueueProvider provider,
            MessageQueueProperties.Destination destination) {
        return provider == MessageQueueProvider.AWS ? destination.getAws() : destination.getAzure();
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) if (value != null) return value;
        return null;
    }

    private PlatformConfigurationException configurationError(String reason) {
        return new PlatformConfigurationException(MessageQueueTechnicalErrors.invalidConfiguration(reason));
    }
}
