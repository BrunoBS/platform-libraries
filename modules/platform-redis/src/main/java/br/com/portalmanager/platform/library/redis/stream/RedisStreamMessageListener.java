package br.com.portalmanager.platform.library.redis.stream;

/** Returning normally acknowledges; throwing keeps the record pending for recovery. */
@FunctionalInterface
public interface RedisStreamMessageListener<T> {
    void onMessage(T event);
}
