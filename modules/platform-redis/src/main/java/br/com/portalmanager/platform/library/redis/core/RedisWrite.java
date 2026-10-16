package br.com.portalmanager.platform.library.redis.core;

import java.time.Duration;

/** Mutable operation builder, single use; false means NX was not satisfied. */
public interface RedisWrite<T> {
    RedisWrite<T> ttl(Duration ttl);

    RedisWrite<T> ifAbsent();

    boolean execute();
}
