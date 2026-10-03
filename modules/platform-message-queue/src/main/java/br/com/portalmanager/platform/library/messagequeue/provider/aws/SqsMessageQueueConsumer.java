package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

        registry.listeners().forEach(listener -> {
            var destination = destinationResolver.resolve(listener.destination());
            if (!destination.consumerEnabled()) {
                return;
            }
            int concurrency = destination.concurrency() == null ? 1 : Math.max(1, destination.concurrency());
            for (int index = 0; index < concurrency; index++) {
                String workerId = listener.destination() + "#" + index;
                workers.put(workerId, executor.submit(() -> poll(listener)));
            }
        });
    }

    private void poll(MessageQueueListenerRegistry.ListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        String queueUrl = queueUrls.computeIfAbsent(destination.queue(), this::resolveQueueUrl);
        int waitTimeSeconds = seconds(destination.waitTime(), 20, 0, 20);
        int visibilityTimeoutSeconds = seconds(destination.visibilityTimeout(), 30, 0, 43200);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(1)
                        .waitTimeSeconds(waitTimeSeconds)
                        .visibilityTimeout(visibilityTimeoutSeconds)
                        .build());

                response.messages().forEach(message ->
                        process(listener, queueUrl, message.body(), message.receiptHandle()));
            } catch (RuntimeException exception) {
                if (!running || Thread.currentThread().isInterrupted()) {
                    break;
                }
                LOGGER.log(System.Logger.Level.WARNING,
                        "Technical failure while polling destination {0}; worker will retry",
                        listener.destination());
                backoff();
            }
        }
    }

    private void process(
            MessageQueueListenerRegistry.ListenerDefinition listener,
            String queueUrl,
            String body,
            String receiptHandle) {
        try {
            var message = serializer.deserialize(body, listener.payloadType());
            listener.invoke(message);
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Message processing failed for destination {0}; message will not be deleted and can be redelivered",
                    listener.destination());
            return;
        }

        try {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Message was processed but could not be deleted from destination {0}; duplicate delivery is possible",
                    listener.destination());
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
