package br.com.portalmanager.platform.library.redis.cache;

import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;

/** JSON only; return type comes from trusted method declarations, never from the payload. */
final class CacheJsonSerializer implements RedisSerializer<Object> {
    private final String region;
    private final CacheReturnTypes types;
    private final RedisJsonCodec codec;

    CacheJsonSerializer(String region, CacheReturnTypes types, RedisJsonCodec codec) {
        this.region = region;
        this.types = types;
        this.codec = codec;
    }

    public byte[] serialize(Object value) {
        if (value == null) return null;
        String json = codec.encode(value);
        codec.decode(json, types.required(region));
        return json.getBytes(StandardCharsets.UTF_8);
    }

    public Object deserialize(byte[] value) {
        return value == null
                ? null
                : codec.decode(new String(value, StandardCharsets.UTF_8), types.required(region));
    }
}
