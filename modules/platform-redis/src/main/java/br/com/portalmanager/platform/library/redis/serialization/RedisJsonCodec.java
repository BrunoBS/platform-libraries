package br.com.portalmanager.platform.library.redis.serialization;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.internal.RedisErrors;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

import java.nio.charset.StandardCharsets;

/** Safe JSON without payload-supplied class names or scalar coercions. */
public final class RedisJsonCodec {
    private final ObjectMapper mapper;
    private final int maxBytes;

    public RedisJsonCodec(ObjectMapper mapper, int maxBytes) {
        this.mapper =
                mapper.rebuild()
                        .deactivateDefaultTyping()
                        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                        .withCoercionConfig(
                                LogicalType.Textual,
                                config ->
                                        config.setCoercion(
                                                        CoercionInputShape.Integer,
                                                        CoercionAction.Fail)
                                                .setCoercion(
                                                        CoercionInputShape.Float,
                                                        CoercionAction.Fail)
                                                .setCoercion(
                                                        CoercionInputShape.Boolean,
                                                        CoercionAction.Fail))
                        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .build();
        this.maxBytes = maxBytes;
    }

    public String encode(Object value) {
        if (value == null || value instanceof JsonNode node && node.isNull())
            throw RedisErrors.invalid();
        try {
            String result = mapper.writeValueAsString(value);
            check(result);
            return result;
        } catch (ApiException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw RedisErrors.error("serialization", failure);
        }
    }

    public <T> T decode(String json, Class<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return decode(json, mapper.constructType(type));
    }

    public <T> T decode(String json, TypeReference<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return decode(json, mapper.constructType(type));
    }

    public <T> T decode(String json, JavaType type) {
        check(json);
        try {
            T result = mapper.readValue(json, type);
            if (result == null) throw RedisErrors.error("serialization", null);
            return result;
        } catch (ApiException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw RedisErrors.error("serialization", failure);
        }
    }

    private void check(String value) {
        if (value == null || value.getBytes(StandardCharsets.UTF_8).length > maxBytes)
            throw RedisErrors.error("limit", null);
    }
}
