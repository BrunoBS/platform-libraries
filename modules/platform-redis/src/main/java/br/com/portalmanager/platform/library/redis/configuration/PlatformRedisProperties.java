package br.com.portalmanager.platform.library.redis.configuration;

import br.com.portalmanager.platform.library.redis.internal.common.RedisErrors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("platform.redis")
public record PlatformRedisProperties(
        @DefaultValue Connection connection,
        @DefaultValue Limits limits,
        @DefaultValue Listener listener,
        @DefaultValue Streams streams,
        @DefaultValue Lifecycle lifecycle) {

    public PlatformRedisProperties {
        if (connection == null || limits == null || listener == null || streams == null || lifecycle == null) {
            throw RedisErrors.configuration();
        }
    }

    public record Connection(
            @DefaultValue("STANDALONE") Topology topology,
            @DefaultValue("localhost") String host,
            @DefaultValue("6379") int port,
            @DefaultValue("0") int database,
            String username,
            String password,
            @DefaultValue("PT60S") Duration commandTimeout,
            @DefaultValue("PT60S") Duration connectTimeout,
            @DefaultValue Ssl ssl,
            @DefaultValue Cluster cluster,
            @DefaultValue Sentinel sentinel) {
        public Connection {
            if (topology == null
                    || host == null
                    || host.isBlank()
                    || port < 1
                    || port > 65535
                    || database < 0
                    || !positive(commandTimeout)
                    || !positive(connectTimeout)
                    || ssl == null
                    || cluster == null
                    || sentinel == null) {
                throw RedisErrors.configuration();
            }
            if (topology == Topology.CLUSTER && cluster.nodes().isEmpty()) {
                throw RedisErrors.configuration();
            }
            if (topology == Topology.SENTINEL
                    && (sentinel.master() == null
                            || sentinel.master().isBlank()
                            || sentinel.nodes().isEmpty())) {
                throw RedisErrors.configuration();
            }
        }
    }

    public enum Topology {
        STANDALONE,
        SENTINEL,
        CLUSTER
    }

    public record Ssl(@DefaultValue("false") boolean enabled, String bundle) {}

    public record Cluster(@DefaultValue List<String> nodes) {
        public Cluster {
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
        }
    }

    public record Sentinel(
            String master,
            @DefaultValue List<String> nodes,
            @DefaultValue("0") int database,
            String username,
            String password) {
        public Sentinel {
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
            if (database < 0) throw RedisErrors.configuration();
        }
    }

    public record Limits(
            @DefaultValue("256") int maxKeyBytes,
            @DefaultValue("1048576") int maxPayloadBytes,
            @DefaultValue("1000") int maxBatchSize,
            @DefaultValue("1000") int maxReadSize) {
        public Limits {
            if (maxKeyBytes < 1
                    || maxKeyBytes > 65536
                    || maxPayloadBytes < 1
                    || maxPayloadBytes > 67108864
                    || maxBatchSize < 1
                    || maxBatchSize > 100000
                    || maxReadSize < 1
                    || maxReadSize > 100000) {
                throw RedisErrors.configuration();
            }
        }
    }

    public record Listener(
            @DefaultValue("16") int threads,
            @DefaultValue("256") int queueCapacity,
            @DefaultValue("PT0.1S") Duration reconnectMin,
            @DefaultValue("PT5S") Duration reconnectMax,
            @DefaultValue("false") boolean keyspaceEnabled) {
        public Listener {
            if (threads < 1
                    || threads > 256
                    || queueCapacity < 1
                    || queueCapacity > 100000
                    || !positive(reconnectMin)
                    || !positive(reconnectMax)
                    || reconnectMax.compareTo(reconnectMin) < 0) {
                throw RedisErrors.configuration();
            }
        }
    }

    public record Streams(
            @DefaultValue("16") int maxWorkers,
            @DefaultValue("10000") long maxLength) {
        public Streams {
            if (maxWorkers < 1 || maxWorkers > 256 || maxLength < 1) {
                throw RedisErrors.configuration();
            }
        }
    }

    public record Lifecycle(@DefaultValue("PT5S") Duration shutdownTimeout) {
        public Lifecycle {
            if (!positive(shutdownTimeout)) throw RedisErrors.configuration();
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
