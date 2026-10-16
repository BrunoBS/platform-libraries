package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.annotation.RedisStreamListener;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import br.com.portalmanager.platform.library.redis.stream.RedisStreamMessageListener;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.MapRecord;

import tools.jackson.databind.JavaType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class RedisStreamsLifecycle implements SmartLifecycle {
    private record Binding(
            RedisStreamMessageListener<Object> listener,
            JavaType type,
            RedisStreamListener settings) {}

    private final ConfigurableListableBeanFactory beans;
    private final DefaultRedisStreams streams;
    private final RedisJsonCodec codec;
    private final RedisAccess access;
    private final ListenerSupport support;
    private ThreadPoolExecutor workers;
    private volatile boolean running;
    private volatile long generation;

    public RedisStreamsLifecycle(
            ConfigurableListableBeanFactory beans,
            DefaultRedisStreams streams,
            RedisJsonCodec codec,
            RedisAccess access,
            ListenerSupport support) {
        this.beans = beans;
        this.streams = streams;
        this.codec = codec;
        this.access = access;
        this.support = support;
    }

    public synchronized void start() {
        if (running) return;
        List<Binding> bindings = new ArrayList<>();
        beans.getBeansOfType(RedisStreamMessageListener.class)
                .forEach(
                        (name, bean) -> {
                            RedisStreamListener settings =
                                    beans.findAnnotationOnBean(name, RedisStreamListener.class);
                            if (settings == null) return;
                            validate(settings);
                            bindings.add(
                                    new Binding(
                                            bean,
                                            support.payloadType(
                                                    bean, RedisStreamMessageListener.class),
                                            settings));
                        });
        if (bindings.isEmpty()) return;
        long count = bindings.stream().mapToLong(binding -> binding.settings().concurrency()).sum();
        if (count > access.properties().maxStreamWorkers()) throw RedisErrors.configuration();
        long currentGeneration = generation + 1;
        String instance = UUID.randomUUID().toString();
        List<Runnable> jobs = new ArrayList<>();
        for (Binding binding : bindings) {
            for (int index = 0; index < binding.settings().concurrency(); index++) {
                String name =
                        binding.settings().consumer().isBlank()
                                ? instance + "-" + index
                                : binding.settings().consumer() + "-" + instance + "-" + index;
                try {
                    access.text(name);
                } catch (
                        br.com.portalmanager.platform.library.messaging.exception
                                        .ValidationException
                                invalid) {
                    throw RedisErrors.configuration();
                }
                jobs.add(() -> consume(binding, name, currentGeneration));
            }
        }
        // Validate all declarations before allocating or starting any consumer.
        workers = support.executor("streams", (int) count, (int) count);
        generation = currentGeneration;
        running = true;
        jobs.forEach(workers::execute);
    }

    private void validate(RedisStreamListener settings) {
        access.key(settings.stream());
        access.text(settings.group());
        access.batch(settings.batchSize());
        access.limit(settings.batchSize());
        long shutdown = access.properties().shutdownTimeout().toMillis();
        if (settings.concurrency() < 1
                || settings.blockMillis() < 1
                || settings.blockMillis() > Math.min(5000, shutdown)
                || settings.minIdleMillis() < 1
                || settings.recoveryIntervalMillis() < 1) throw RedisErrors.configuration();
        streams.retention(
                settings.maxLength() == 0
                        ? access.properties().streamMaxLength()
                        : settings.maxLength());
    }

    private void consume(Binding binding, String consumer, long currentGeneration) {
        var settings = binding.settings();
        boolean initialized = false;
        long recoverAt = 0;
        String recoveryCursor = "0-0";
        var backoff = support.backoff().start();
        while (active(currentGeneration) && !Thread.currentThread().isInterrupted()) {
            try {
                if (!initialized) {
                    streams.createGroup(settings.stream(), settings.group(), settings.start());
                    initialized = true;
                }
                long now = System.nanoTime();
                if (now >= recoverAt) {
                    var recovered =
                            streams.recoverRaw(
                                    settings.stream(),
                                    settings.group(),
                                    consumer,
                                    recoveryCursor,
                                    Duration.ofMillis(settings.minIdleMillis()),
                                    settings.batchSize());
                    recoveryCursor = recovered.nextId();
                    process(binding, currentGeneration, recovered.records());
                    streams.trim(
                            settings.stream(),
                            settings.maxLength() == 0
                                    ? access.properties().streamMaxLength()
                                    : settings.maxLength());
                    recoverAt =
                            now + TimeUnit.MILLISECONDS.toNanos(settings.recoveryIntervalMillis());
                }
                process(
                        binding,
                        currentGeneration,
                        streams.readGroupRaw(
                                settings.stream(),
                                settings.group(),
                                consumer,
                                settings.batchSize(),
                                Duration.ofMillis(settings.blockMillis())));
                backoff = support.backoff().start();
            } catch (RuntimeException failure) {
                if (!active(currentGeneration)) return;
                support.failed("stream", failure);
                initialized = false;
                try {
                    Thread.sleep(backoff.nextBackOff());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void process(
            Binding binding,
            long currentGeneration,
            List<MapRecord<String, String, String>> records) {
        if (records == null) return;
        for (var record : records) {
            if (!active(currentGeneration)) return;
            try {
                Object payload = codec.decode(record.getValue().get("payload"), binding.type());
                binding.listener().onMessage(payload);
                if (active(currentGeneration))
                    streams.acknowledge(
                            binding.settings().stream(),
                            binding.settings().group(),
                            List.of(record.getId().getValue()));
                support.processed();
            } catch (RuntimeException failure) {
                support.failed("stream", failure);
            }
        }
    }

    private boolean active(long currentGeneration) {
        return running && generation == currentGeneration;
    }

    public synchronized void stop() {
        running = false;
        generation++;
        support.shutdown(workers);
        workers = null;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
