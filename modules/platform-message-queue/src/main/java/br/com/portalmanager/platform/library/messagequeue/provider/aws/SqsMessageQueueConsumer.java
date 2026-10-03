package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.DeadLetterMessage;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageQueueConfigurationException;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
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

    private static final System.Logger LOGGER = System.getLogger(SqsMessageQueueConsumer.class.getName());

    private final SqsClient sqsClient;
    private final MessageQueueListenerRegistry registry;
    private final DestinationResolver destinationResolver;
    private final MessageQueueSerializer serializer;
    private final MessageQueueProperties properties;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> workers = new ConcurrentHashMap<>();

    private volatile ExecutorService executor;
    private volatile boolean running;

    public SqsMessageQueueConsumer(
            SqsClient sqsClient,
            MessageQueueListenerRegistry registry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer serializer,
            MessageQueueProperties properties) {
        this.sqsClient = sqsClient;
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

        registry.listeners().forEach(this::startListenerWorkers);
        registry.deadLetterListeners().forEach(this::startDeadLetterWorker);
    }

    private void startListenerWorkers(MessageQueueListenerRegistry.ListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        if (!destination.consumerEnabled()) {
            return;
        }

        int concurrency = destination.concurrency() == null ? 1 : Math.max(1, destination.concurrency());
        for (int index = 0; index < concurrency; index++) {
            String workerId = listener.destination() + "#consumer#" + index;
            workers.put(workerId, executor.submit(() -> poll(
                    listener.destination(),
                    destination.queue(),
                    destination.waitTime(),
                    destination.visibilityTimeout(),
                    (queueUrl, message) -> process(listener, queueUrl, message))));
        }
    }

    private void startDeadLetterWorker(MessageQueueListenerRegistry.DeadLetterListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        if (destination.deadLetterReference() == null || destination.deadLetterReference().isBlank()) {
            throw new MessageQueueConfigurationException(
                    "AWS dead-letter queue is required for destination with dead-letter listener: "
                            + listener.destination());
        }

        String deadLetterQueue = destination.deadLetterReference();
        String workerId = listener.destination() + "#dead-letter";
        workers.put(workerId, executor.submit(() -> poll(
                listener.destination(),
                deadLetterQueue,
                destination.waitTime(),
                destination.visibilityTimeout(),
                (queueUrl, message) -> processDeadLetter(
                        listener,
                        deadLetterQueue,
                        queueUrl,
                        message))));
    }

    private void poll(
            String destination,
            String queueName,
            Duration waitTime,
            Duration visibilityTimeout,
            BiConsumer<String, Message> messageProcessor) {
        String queueUrl = queueUrls.computeIfAbsent(queueName, this::resolveQueueUrl);
        int waitTimeSeconds = seconds(waitTime, 20, 0, 20);
        int visibilityTimeoutSeconds = seconds(visibilityTimeout, 30, 0, 43200);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(1)
                        .waitTimeSeconds(waitTimeSeconds)
                        .visibilityTimeout(visibilityTimeoutSeconds)
                        .build());

                if (!running || Thread.currentThread().isInterrupted()) {
                    break;
                }

                response.messages().forEach(message -> messageProcessor.accept(queueUrl, message));
            } catch (RuntimeException exception) {
                if (!running || Thread.currentThread().isInterrupted()) {
                    break;
                }
                LOGGER.log(System.Logger.Level.WARNING,
                        "Technical failure while polling destination " + destination + "; worker will retry",
                        exception);
                backoff();
            }
        }
    }

    private void process(
            MessageQueueListenerRegistry.ListenerDefinition listener,
            String queueUrl,
            Message receivedMessage) {
        try {
            var message = serializer.deserialize(receivedMessage.body(), listener.payloadType());
            listener.invoke(message);
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Message processing failed for destination " + listener.destination()
                            + "; message will not be deleted and can be redelivered",
                    exception);
            return;
        }

        deleteProcessedMessage(listener.destination(), queueUrl, receivedMessage.receiptHandle());
    }

    private void processDeadLetter(
            MessageQueueListenerRegistry.DeadLetterListenerDefinition listener,
            String deadLetterQueue,
            String queueUrl,
            Message receivedMessage) {
        try {
            var message = serializer.deserialize(receivedMessage.body(), listener.payloadType());
            var deadLetterMessage = new DeadLetterMessage<>(
                    message,
                    null,
                    null,
                    null,
                    null,
                    Map.of("provider", "AWS", "queue", deadLetterQueue));
            listener.invoke(deadLetterMessage);
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Dead-letter processing failed for destination " + listener.destination()
                            + "; message will remain in the dead-letter queue",
                    exception);
            return;
        }

        deleteProcessedMessage(listener.destination() + " dead-letter", queueUrl, receivedMessage.receiptHandle());
    }

    private void deleteProcessedMessage(String destination, String queueUrl, String receiptHandle) {
        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Message was processed but could not be deleted from destination " + destination
                            + "; duplicate delivery is possible",
                    exception);
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
