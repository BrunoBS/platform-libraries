package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.pubsub.RedisPublisher;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.data.redis.core.StringRedisTemplate;

public final class DefaultRedisPublisher implements RedisPublisher {
    private final StringRedisTemplate redis;
    private final RedisJsonCodec codec;
    private final RedisAccess access;

    public DefaultRedisPublisher(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
    }

    public <T> long publish(String channel, T payload) {
        return RedisErrors.call(
                () -> redis.convertAndSend(access.text(channel), codec.encode(payload)));
    }
}
