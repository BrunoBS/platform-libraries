package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.monitoring.MessageQueueMetrics;
import br.com.portalmanager.platform.library.messagequeue.contract.DeadLetterMessage;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueTechnicalErrors;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class SqsMessageQueueConsumer implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsMessageQueueConsumer.class);

    private final SqsClient sqsClient;
    private final MessageQueueListenerRegistry registry;
    private final DestinationResolver destinationResolver;
    private final MessageQueueSerializer serializer;
    private final MessageQueueProperties properties;
    private final MessageQueueMetrics metrics;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> workers = new ConcurrentHashMap<>();

    private volatile ExecutorService executor;
    private volatile boolean running;

    public SqsMessageQueueConsumer(
            SqsClient sqsClient,
            MessageQueueListenerRegistry registry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer serializer,
            MessageQueueProperties properties,
            MessageQueueMetrics metrics) {
        this.sqsClient = sqsClient;
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

        registry.listeners().forEach(this::startListenerWorkers);
        registry.deadLetterListeners().forEach(this::startDeadLetterWorker);
    }

    private void startListenerWorkers(MessageQueueListenerRegistry.ListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        if (!destination.consumerEnabled()) {
            return;
        }

        int concurrency = destination.concurrency() == null ? 1 : destination.concurrency();
        for (int index = 0; index < concurrency; index++) {
            String workerId = listener.destination() + "#consumer#" + index;
            workers.put(workerId, executor.submit(() -> poll(
                    listener.destination(),
                    destination.queue(),
                    destination.waitTime(),
                    destination.visibilityTimeout(),
                    false,
                    (queueUrl, message) -> process(listener, queueUrl, message))));
        }
    }

    private void startDeadLetterWorker(MessageQueueListenerRegistry.DeadLetterListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        if (!destination.consumerEnabled()) {
            return;
        }
        if (destination.deadLetterReference() == null || destination.deadLetterReference().isBlank()) {
            throw new PlatformConfigurationException(MessageQueueTechnicalErrors.invalidConfiguration(
                    "AWS dead-letter queue is required for destination with dead-letter listener: "
                            + listener.destination()));
        }

        String deadLetterQueue = destination.deadLetterReference();
        int concurrency = destination.concurrency() == null ? 1 : destination.concurrency();
        for (int index = 0; index < concurrency; index++) {
            String workerId = listener.destination() + "#dead-letter#" + index;
            workers.put(workerId, executor.submit(() -> poll(
                    listener.destination(),
                    deadLetterQueue,
                    destination.waitTime(),
                    destination.visibilityTimeout(),
                    true,
                    (queueUrl, message) -> processDeadLetter(
                            listener,
                            deadLetterQueue,
                            queueUrl,
                            message))));
        }
    }

    private void poll(
            String destination,
            String queueName,
            Duration waitTime,
            Duration visibilityTimeout,
            boolean deadLetter,
            BiConsumer<String, Message> messageProcessor) {
        int waitTimeSeconds = seconds(waitTime, 20, 0, 20);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String queueUrl = queueUrls.computeIfAbsent(queueName, this::resolveQueueUrl);
                var request = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(1)
                        .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                        .waitTimeSeconds(waitTimeSeconds);
                if (visibilityTimeout != null) {
                    request.visibilityTimeout(seconds(visibilityTimeout, 30, 0, 43200));
                }
                var response = sqsClient.receiveMessage(request.build());

                if (!running || Thread.currentThread().isInterrupted()) {
                    break;
                }

                response.messages().forEach(message -> messageProcessor.accept(queueUrl, message));
            } catch (RuntimeException exception) {
                if (!running || Thread.currentThread().isInterrupted()) {
                    break;
                }
                metrics.recordPollFailure(MessageQueueProvider.AWS, destination, deadLetter);
                LOGGER.warn("Technical failure while polling destination {}; worker will retry", destination, exception);
                backoff();
            }
        }
    }

    private void process(
            MessageQueueListenerRegistry.ListenerDefinition listener,
            String queueUrl,
            Message receivedMessage) {
        String messageId = null;
        String correlationId = null;
        try {
            var message = serializer.deserialize(receivedMessage.body(), listener.payloadType());
            messageId = message.messageId();
            correlationId = message.correlationId();
            listener.invoke(message);
            metrics.recordConsume(MessageQueueProvider.AWS, listener.destination(), false, true);
        } catch (RuntimeException exception) {
            metrics.recordConsume(MessageQueueProvider.AWS, listener.destination(), false, false);
            LOGGER.warn(
                    "Message processing failed for destination {}; messageId={} correlationId={}; "
                            + "message will not be deleted and can be redelivered",
                    listener.destination(), messageId, correlationId, exception);
            return;
        }

        deleteProcessedMessage(
                listener.destination(), queueUrl, receivedMessage.receiptHandle(), messageId, correlationId, false);
    }

    private void processDeadLetter(
            MessageQueueListenerRegistry.DeadLetterListenerDefinition listener,
            String deadLetterQueue,
            String queueUrl,
            Message receivedMessage) {
        String messageId = null;
        String correlationId = null;
        try {
            var message = serializer.deserialize(receivedMessage.body(), listener.payloadType());
            messageId = message.messageId();
            correlationId = message.correlationId();
            Integer deliveryCount = approximateReceiveCount(receivedMessage);
            var deadLetterMessage = new DeadLetterMessage<>(
                    message,
                    null,
                    null,
                    deliveryCount,
                    null,
                    Map.of("provider", "AWS", "queue", deadLetterQueue));
            listener.invoke(deadLetterMessage);
            metrics.recordConsume(MessageQueueProvider.AWS, listener.destination(), true, true);
        } catch (RuntimeException exception) {
            metrics.recordConsume(MessageQueueProvider.AWS, listener.destination(), true, false);
            LOGGER.warn(
                    "Dead-letter processing failed for destination {}; messageId={} correlationId={}; "
                            + "message will remain in the dead-letter queue",
                    listener.destination(), messageId, correlationId, exception);
            return;
        }

        deleteProcessedMessage(
                listener.destination(),
                queueUrl, receivedMessage.receiptHandle(), messageId, correlationId, true);
    }

    private Integer approximateReceiveCount(Message message) {
        String value = message.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Invalid SQS ApproximateReceiveCount value: {}", value, exception);
            return null;
        }
    }

    private void deleteProcessedMessage(
            String destination,
            String queueUrl,
            String receiptHandle,
            String messageId,
            String correlationId,
            boolean deadLetter) {
        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());
        } catch (RuntimeException exception) {
            metrics.recordAcknowledgementFailure(MessageQueueProvider.AWS, destination, deadLetter);
            LOGGER.warn(
                    "Message was processed but could not be deleted from destination {}; "
                            + "messageId={} correlationId={}; duplicate delivery is possible",
                    destination, messageId, correlationId, exception);
        }
    }

    private String resolveQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .queueUrl();
    }

    private int seconds(Duration value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        long seconds = value.toSeconds();
        if (seconds < min || seconds > max) {
            throw new IllegalArgumentException("Duration seconds must be between " + min + " and " + max);
        }
        return Math.toIntExact(seconds);
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
            return;
        }

        currentExecutor.shutdown();
        Duration shutdownTimeout = properties.getShutdownTimeout();
        long timeoutMillis = shutdownTimeout == null ? 30_000L : Math.max(0L, shutdownTimeout.toMillis());

        try {
            if (!currentExecutor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS)) {
                workers.values().forEach(worker -> worker.cancel(true));
                currentExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            workers.values().forEach(worker -> worker.cancel(true));
            currentExecutor.shutdownNow();
        } finally {
            workers.clear();
            executor = null;
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
}
