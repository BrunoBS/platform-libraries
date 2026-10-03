package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.DeadLetterMessage;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
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

    private static final System.Logger LOGGER = System.getLogger(AzureServiceBusMessageQueueConsumer.class.getName());

    private final ServiceBusClientBuilder clientBuilder;
    private final MessageQueueListenerRegistry registry;
    private final DestinationResolver destinationResolver;
    private final MessageQueueSerializer serializer;
    private final MessageQueueProperties properties;
    private final Map<String, Future<?>> workers = new ConcurrentHashMap<>();
    private final Map<String, ServiceBusReceiverClient> receivers = new ConcurrentHashMap<>();

    private volatile ExecutorService executor;
    private volatile boolean running;

    public AzureServiceBusMessageQueueConsumer(
            ServiceBusClientBuilder clientBuilder,
            MessageQueueListenerRegistry registry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer serializer,
            MessageQueueProperties properties) {
        this.clientBuilder = clientBuilder;
        this.registry = registry;
        this.destinationResolver = destinationResolver;
        this.serializer = serializer;
        this.properties = properties;
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
        poll(workerId, destination.queue(), false, destination.waitTime(),
                (receiver, message) -> receiveAndProcess(receiver, listener, message));
    }

    private void pollDeadLetter(String workerId, MessageQueueListenerRegistry.DeadLetterListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        poll(workerId, destination.queue(), true, destination.waitTime(),
                (receiver, message) -> receiveAndProcessDeadLetter(receiver, listener, message));
    }

    private void poll(String workerId, String queueName, boolean deadLetter, Duration waitTime, ReceiverWork work) {
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
                LOGGER.log(System.Logger.Level.WARNING,
                        "Technical failure while polling Azure Service Bus queue " + queueName + "; worker will retry",
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
                .disableAutoComplete();
        if (deadLetter) {
            builder.subQueue(SubQueue.DEAD_LETTER_QUEUE);
        }
        return builder.buildClient();
    }

    private void receiveAndProcess(
            ServiceBusReceiverClient receiver,
            MessageQueueListenerRegistry.ListenerDefinition listener,
            ServiceBusReceivedMessage received) {
        try {
            var message = serializer.deserialize(received.getBody().toString(), listener.payloadType());
            listener.invoke(message);
            complete(receiver, received, listener.destination());
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Message processing failed for Azure destination " + listener.destination()
                            + "; message will be abandoned for redelivery",
                    exception);
            abandon(receiver, received, listener.destination());
        }
    }

    private void receiveAndProcessDeadLetter(
            ServiceBusReceiverClient receiver,
            MessageQueueListenerRegistry.DeadLetterListenerDefinition listener,
            ServiceBusReceivedMessage received) {
        try {
            var message = serializer.deserialize(received.getBody().toString(), listener.payloadType());
            var deadLetterMessage = new DeadLetterMessage<>(
                    message,
                    received.getDeadLetterReason(),
                    received.getDeadLetterErrorDescription(),
                    Math.toIntExact(received.getDeliveryCount()),
                    null,
                    providerMetadata(received));
            listener.invoke(deadLetterMessage);
            complete(receiver, received, listener.destination());
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Dead-letter message processing failed for Azure destination " + listener.destination()
                            + "; message will be abandoned in the dead-letter subqueue",
                    exception);
            abandon(receiver, received, listener.destination());
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

    private void complete(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message, String destination) {
        try {
            receiver.complete(message);
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Message was processed but could not be completed for Azure destination " + destination
                            + "; duplicate delivery is possible",
                    exception);
        }
    }

    private void abandon(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message, String destination) {
        try {
            receiver.abandon(message);
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Failed to abandon Azure message for destination " + destination
                            + "; broker lock expiry will determine redelivery",
                    exception);
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
            LOGGER.log(System.Logger.Level.WARNING, "Failed to close Azure Service Bus receiver", exception);
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
