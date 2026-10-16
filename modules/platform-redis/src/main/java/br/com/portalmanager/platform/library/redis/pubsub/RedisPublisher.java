package br.com.portalmanager.platform.library.redis.pubsub;

/** Returns subscribers reported by Redis, never a processing acknowledgement. */
public interface RedisPublisher {
    <T> long publish(String channel, T payload);
}
