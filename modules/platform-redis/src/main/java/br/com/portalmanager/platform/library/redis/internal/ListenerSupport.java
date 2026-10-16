package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.core.RedisListenerErrorHandler;
import br.com.portalmanager.platform.library.redis.core.RedisListenerMetrics;

import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ListenerSupport {
    private final RedisListenerErrorHandler errors;
    private final RedisListenerMetrics metrics;
    private final PlatformRedisProperties properties;

    public ListenerSupport(
            RedisListenerErrorHandler errors,
            RedisListenerMetrics metrics,
            PlatformRedisProperties properties) {
        this.errors = errors;
        this.metrics = metrics;
        this.properties = properties;
    }

    public JavaType payloadType(Object bean, Class<?> contract) {
        Class<?> target = AopUtils.getTargetClass(bean);
        ResolvableType type = ResolvableType.forClass(target).as(contract).getGeneric(0);
        return javaType(type);
    }

    private JavaType javaType(ResolvableType type) {
        if (type.resolve() == null || type.hasUnresolvableGenerics())
            throw RedisErrors.configuration();
        var factory = TypeFactory.createDefaultInstance();
        if (type.isArray()) return factory.constructArrayType(javaType(type.getComponentType()));
        if (!type.hasGenerics()) return factory.constructType(type.resolve());
        var arguments =
                Arrays.stream(type.getGenerics()).map(this::javaType).toArray(JavaType[]::new);
        return factory.constructParametricType(type.resolve(), arguments);
    }

    public void processed() {
        metrics.processed();
    }

    public void failed(String transport, Throwable failure) {
        report(transport, RedisErrors.error("listener", failure));
    }

    public void report(String transport, ApiException failure) {
        metrics.failed();
        try {
            errors.onError(transport, failure);
        } catch (RuntimeException ignored) {
            LoggerFactory.getLogger(ListenerSupport.class)
                    .error("PLT-REDIS-009 error handler failed");
        }
    }

    public ThreadPoolExecutor executor(String transport, int threads, int capacity) {
        return new ThreadPoolExecutor(
                threads,
                threads,
                0,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity),
                Thread.ofPlatform()
                        .daemon(true)
                        .name("platform-redis-" + transport + "-", 0)
                        .factory(),
                (task, executor) -> {
                    metrics.rejected();
                    report(transport, RedisErrors.error("saturation", null));
                });
    }

    public BackOff backoff() {
        return () ->
                new BackOffExecution() {
                    private long delay = properties.reconnectMin().toMillis();

                    public long nextBackOff() {
                        long base = delay;
                        delay =
                                Math.min(
                                        properties.reconnectMax().toMillis(),
                                        delay > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : delay * 2);
                        return ThreadLocalRandom.current()
                                .nextLong(Math.max(1, base / 2), base + 1);
                    }
                };
    }

    public void shutdown(ExecutorService executor) {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(
                    properties.shutdownTimeout().toMillis(), TimeUnit.MILLISECONDS))
                executor.shutdownNow();
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
