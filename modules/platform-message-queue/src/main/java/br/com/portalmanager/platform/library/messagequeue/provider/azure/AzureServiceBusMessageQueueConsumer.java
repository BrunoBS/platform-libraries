package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.monitoring.MessageQueueMetrics;
import br.com.portalmanager.platform.library.messagequeue.contract.DeadLetterMessage;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AzureServiceBusMessageQueueConsumer implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureServiceBusMessageQueueConsumer.class);

    private final ServiceBusClientBuilder clientBuilder;
    private final MessageQueueListenerRegistry registry;
    private final DestinationResolver destinationResolver;
    private final MessageQueueSerializer serializer;
    private final MessageQueueProperties properties;
    private final MessageQueueMetrics metrics;
    private final Map<String, Future<?>> workers = new ConcurrentHashMap<>();
    private final Map<String, ServiceBusReceiverClient> receivers = new ConcurrentHashMap<>();

    private volatile ExecutorService executor;
    private volatile boolean running;

    public AzureServiceBusMessageQueueConsumer(
            ServiceBusClientBuilder clientBuilder,
            MessageQueueListenerRegistry registry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer serializer,
            MessageQueueProperties properties,
            MessageQueueMetrics metrics) {
        this.clientBuilder = clientBuilder;
        this.registry = registry;
        this.destinationResolver = destinationResolver;
        this.serializer = serializer;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        executor = Executors.newVirtualThreadPerTaskExecutor();
        running = true;

        registry.listeners().forEach(listener -> startListenerWorkers(listener));
        registry.deadLetterListeners().forEach(listener -> startDeadLetterWorkers(listener));
    }

    private void startListenerWorkers(MessageQueueListenerRegistry.ListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        if (!destination.consumerEnabled()) {
            return;
        }
        int concurrency = concurrency(destination.concurrency());
        for (int index = 0; index < concurrency; index++) {
            String workerId = "listener:" + listener.destination() + "#" + index;
            workers.put(workerId, executor.submit(() -> pollListener(workerId, listener)));
        }
    }

    private void startDeadLetterWorkers(MessageQueueListenerRegistry.DeadLetterListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        if (!destination.consumerEnabled()) {
            return;
        }
        int concurrency = concurrency(destination.concurrency());
        for (int index = 0; index < concurrency; index++) {
            String workerId = "dead-letter:" + listener.destination() + "#" + index;
            workers.put(workerId, executor.submit(() -> pollDeadLetter(workerId, listener)));
        }
    }

    private void pollListener(String workerId, MessageQueueListenerRegistry.ListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        poll(workerId, listener.destination(), destination.queue(), false, destination.waitTime(),
                (receiver, message) -> receiveAndProcess(receiver, listener, message));
    }

    private void pollDeadLetter(String workerId, MessageQueueListenerRegistry.DeadLetterListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        poll(workerId, listener.destination(), destination.queue(), true, destination.waitTime(),
                (receiver, message) -> receiveAndProcessDeadLetter(receiver, listener, message));
    }

    private void poll(
            String workerId,
            String destination,
            String queueName,
            boolean deadLetter,
            Duration waitTime,
            ReceiverWork work) {
        if (waitTime == null || waitTime.isZero() || waitTime.isNegative()) {
            waitTime = Duration.ofSeconds(20);
        }

        while (running && !Thread.currentThread().isInterrupted()) {
            ServiceBusReceiverClient receiver = null;
            try {
                receiver = createReceiver(queueName, deadLetter);
                receivers.put(workerId, receiver);

                while (running && !Thread.currentThread().isInterrupted()) {
                    var messages = receiver.receiveMessages(1, waitTime);
                    if (!running || Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    for (ServiceBusReceivedMessage message : messages) {
                        work.process(receiver, message);
                    }
                }
            } catch (RuntimeException exception) {
                if (!running || Thread.currentThread().isInterrupted()) {
                    break;
                }
                metrics.recordPollFailure(MessageQueueProvider.AZURE, destination, deadLetter);
                LOGGER.warn(
                        "Technical failure while polling Azure Service Bus queue {}; worker will retry",
                        queueName,
                        exception);
                backoff();
            } finally {
                receivers.remove(workerId);
                close(receiver);
            }
        }
    }

    private ServiceBusReceiverClient createReceiver(String queueName, boolean deadLetter) {
        var builder = clientBuilder.receiver()
                .queueName(queueName)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete()
                .maxAutoLockRenewDuration(properties.getAzure().getMaxAutoLockRenewalDuration());
        if (deadLetter) {
            builder.subQueue(SubQueue.DEAD_LETTER_QUEUE);
        }
        return builder.buildClient();
    }

    private void receiveAndProcess(
            ServiceBusReceiverClient receiver,
            MessageQueueListenerRegistry.ListenerDefinition listener,
            ServiceBusReceivedMessage received) {
        String messageId = null;
        String correlationId = null;
        try {
            var message = serializer.deserialize(received.getBody().toString(), listener.payloadType());
            messageId = message.messageId();
            correlationId = message.correlationId();
            listener.invoke(message);
            metrics.recordConsume(MessageQueueProvider.AZURE, listener.destination(), false, true);
            complete(receiver, received, listener.destination(), message, false);
        } catch (RuntimeException exception) {
            metrics.recordConsume(MessageQueueProvider.AZURE, listener.destination(), false, false);
            LOGGER.warn(
                    "Message processing failed for Azure destination {}; messageId={} correlationId={}; "
                            + "message will be abandoned for redelivery",
                    listener.destination(), messageId, correlationId, exception);
            abandon(receiver, received, listener.destination(), messageId, correlationId);
        }
    }

    private void receiveAndProcessDeadLetter(
            ServiceBusReceiverClient receiver,
            MessageQueueListenerRegistry.DeadLetterListenerDefinition listener,
            ServiceBusReceivedMessage received) {
        String messageId = null;
        String correlationId = null;
        try {
            var message = serializer.deserialize(received.getBody().toString(), listener.payloadType());
            messageId = message.messageId();
            correlationId = message.correlationId();
            var deadLetterMessage = new DeadLetterMessage<>(
                    message,
                    received.getDeadLetterReason(),
                    received.getDeadLetterErrorDescription(),
                    Math.toIntExact(received.getDeliveryCount()),
                    null,
                    providerMetadata(received));
            listener.invoke(deadLetterMessage);
            metrics.recordConsume(MessageQueueProvider.AZURE, listener.destination(), true, true);
            complete(receiver, received, listener.destination(), message, true);
        } catch (RuntimeException exception) {
            metrics.recordConsume(MessageQueueProvider.AZURE, listener.destination(), true, false);
            LOGGER.warn(
                    "Dead-letter message processing failed for Azure destination {}; "
                            + "messageId={} correlationId={}; message will be abandoned in the dead-letter subqueue",
                    listener.destination(), messageId, correlationId, exception);
            abandon(receiver, received, listener.destination(), messageId, correlationId);
        }
    }

    private Map<String, String> providerMetadata(ServiceBusReceivedMessage received) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("provider", "AZURE");
        metadata.put("sequenceNumber", Long.toString(received.getSequenceNumber()));
        if (received.getDeadLetterSource() != null) {
            metadata.put("deadLetterSource", received.getDeadLetterSource());
        }
        return Map.copyOf(metadata);
    }

    private void complete(
            ServiceBusReceiverClient receiver,
            ServiceBusReceivedMessage received,
            String destination,
            MessageQueueMessage<?> envelope,
            boolean deadLetter) {
        try {
            receiver.complete(received);
        } catch (RuntimeException exception) {
            metrics.recordAcknowledgementFailure(MessageQueueProvider.AZURE, destination, deadLetter);
            LOGGER.warn(
                    "Message was processed but could not be completed for Azure destination {}; "
                            + "messageId={} correlationId={}; duplicate delivery is possible",
                    destination, envelope.messageId(), envelope.correlationId(), exception);
        }
    }

    private void abandon(
            ServiceBusReceiverClient receiver,
            ServiceBusReceivedMessage received,
            String destination,
            String messageId,
            String correlationId) {
        try {
            receiver.abandon(received);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Failed to abandon Azure message for destination {}; messageId={} correlationId={}; "
                            + "broker lock expiry will determine redelivery",
                    destination, messageId, correlationId, exception);
        }
    }

    private int concurrency(Integer configured) {
        return configured == null ? 1 : Math.max(1, configured);
    }

    private void backoff() {
        Duration backoff = properties.getPollFailureBackoff();
        if (backoff == null || backoff.isZero() || backoff.isNegative()) {
            return;
        }
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;

        ExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            workers.clear();
            closeReceivers();
            return;
        }

        currentExecutor.shutdown();
        Duration shutdownTimeout = properties.getShutdownTimeout();
        long timeoutMillis = shutdownTimeout == null ? 30_000L : Math.max(0L, shutdownTimeout.toMillis());

        try {
            if (!currentExecutor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                closeReceivers();
                workers.values().forEach(worker -> worker.cancel(true));
                currentExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            closeReceivers();
            workers.values().forEach(worker -> worker.cancel(true));
            currentExecutor.shutdownNow();
        } finally {
            workers.clear();
            closeReceivers();
            executor = null;
        }
    }

    private void closeReceivers() {
        receivers.values().forEach(this::close);
        receivers.clear();
    }

    private void close(ServiceBusReceiverClient receiver) {
        if (receiver == null) {
            return;
        }
        try {
            receiver.close();
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to close Azure Service Bus receiver", exception);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @FunctionalInterface
    private interface ReceiverWork {
        void process(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message);
    }
}
