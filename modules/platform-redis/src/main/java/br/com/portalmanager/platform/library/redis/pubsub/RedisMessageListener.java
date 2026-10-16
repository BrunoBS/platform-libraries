package br.com.portalmanager.platform.library.redis.pubsub;

/** Pub/Sub is transient; disconnected subscribers lose messages. */
@FunctionalInterface
public interface RedisMessageListener<T> {
    void onMessage(T event);
}
