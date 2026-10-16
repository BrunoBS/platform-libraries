package br.com.portalmanager.platform.library.redis.configuration;

import br.com.portalmanager.platform.library.redis.internal.RedisErrors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("platform.redis")
public record PlatformRedisProperties(
        @DefaultValue("") String namespace,
        @DefaultValue("ISOLATED") NamespaceMode namespaceMode,
        @DefaultValue("256") int maxKeyBytes,
        @DefaultValue("1048576") int maxPayloadBytes,
        @DefaultValue("1000") int maxBatchSize,
        @DefaultValue("1000") int maxReadSize,
        @DefaultValue("4") int listenerThreads,
        @DefaultValue("256") int listenerQueueCapacity,
        @DefaultValue("16") int maxStreamWorkers,
        @DefaultValue("10000") long streamMaxLength,
        @DefaultValue("PT5S") Duration shutdownTimeout,
        @DefaultValue("PT0.1S") Duration reconnectMin,
        @DefaultValue("PT5S") Duration reconnectMax,
        @DefaultValue("false") boolean keyspaceEnabled) {
    public enum NamespaceMode {
        ISOLATED,
        SHARED
    }

    public PlatformRedisProperties {
        if (namespaceMode == null
                || maxKeyBytes < 1
                || maxKeyBytes > 65536
                || maxPayloadBytes < 1
                || maxPayloadBytes > 67108864
                || maxBatchSize < 1
                || maxBatchSize > 100000
                || maxReadSize < 1
                || maxReadSize > 100000
                || listenerThreads < 1
                || listenerThreads > 256
                || listenerQueueCapacity < 1
                || listenerQueueCapacity > 100000
                || maxStreamWorkers < 1
                || maxStreamWorkers > 256
                || streamMaxLength < 1
                || !positive(shutdownTimeout)
                || !positive(reconnectMin)
                || !positive(reconnectMax)
                || reconnectMax.compareTo(reconnectMin) < 0) {
            throw RedisErrors.configuration();
        }
    }

    private static boolean positive(Duration value) {
        try {
            return value != null && value.toMillis() > 0 && value.toMillis() < Long.MAX_VALUE;
        } catch (ArithmeticException failure) {
            return false;
        }
    }
}
