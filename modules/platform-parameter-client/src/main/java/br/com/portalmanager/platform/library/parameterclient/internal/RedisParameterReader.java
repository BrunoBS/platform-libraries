package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterFields;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterType;
import br.com.portalmanager.platform.library.parameterclient.internal.serialization.ExactDocument;
import br.com.portalmanager.platform.library.parameterclient.message.ParameterClientMessageKeys;
import br.com.portalmanager.platform.library.redis.store.RedisStore;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/** Shared Redis read/decoding boundary; providers never expose a RedisStore. */
public final class RedisParameterReader {
    private static final String NAMESPACE = "dynamic-parameter";
    private final RedisStore scopedStore;
    private final ObjectMapper mapper;

    public RedisParameterReader(RedisStore store, ObjectMapper mapper) {
        this.mapper =
                Objects.requireNonNull(mapper)
                        .rebuild()
                        .deactivateDefaultTyping()
                        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                        .withCoercionConfig(
                                LogicalType.Textual,
                                config ->
                                        config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                                                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                                                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                        .build();
        this.scopedStore = Objects.requireNonNull(store).withNamespace(NAMESPACE);
    }

    public static void validateKey(String key) {
        if (key == null || key.isBlank()) throw ParameterErrors.invalid();
    }

    public <T> T configuration(String key, Class<T> type, T defaultValue) {
        Objects.requireNonNull(type);
        JavaType targetType = mapper.constructType(type);
        return configuration(key, defaultValue, value -> mapper.convertValue(value, targetType));
    }

    public <T> T configuration(String key, TypeReference<T> type, T defaultValue) {
        Objects.requireNonNull(type);
        JavaType targetType = mapper.constructType(type);
        return configuration(key, defaultValue, value -> mapper.convertValue(value, targetType));
    }

    private <T> T configuration(String key, T defaultValue, Function<JsonNode, T> convert) {
        validateKey(key);
        Optional<ExactDocument> document;
        try {
            document = read(key, ExactDocument.class);
        } catch (RuntimeException failure) {
            // Keep CONFIGURATION's existing controlled read-failure semantics.
            if (failure instanceof ApiException api
                    && api.getMessageKey().startsWith(ParameterClientMessageKeys.PREFIX)) throw api;
            throw ParameterErrors.readFailure(failure);
        }
        if (document.isEmpty()) return defaultValue;
        JsonNode value = configurationValue(document.get().root());
        try {
            return convert.apply(value);
        } catch (RuntimeException failure) {
            // Never retain payload-bearing conversion exceptions.
            throw ParameterErrors.valueInvalid();
        }
    }

    public JsonNode expression(String key) {
        return read(key, ExactDocument.class).orElseThrow(ParameterErrors::missing).root();
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        try {
            return scopedStore.get(key, type);
        } catch (ApiException failure) {
            if (failure.getMessageKey().startsWith(ParameterClientMessageKeys.PREFIX)) throw failure;
            throw ParameterErrors.readFailure(failure);
        }
    }

    private JsonNode configurationValue(JsonNode envelope) {
        if (!envelope.isObject()) throw ParameterErrors.valueInvalid();
        JsonNode type = envelope.get(ParameterFields.TYPE);
        if (type == null || !type.isString()
                || !ParameterType.CONFIGURATION.name().equals(type.asString())) {
            throw ParameterErrors.valueInvalid();
        }
        JsonNode value = envelope.get(ParameterFields.VALUE);
        if (value == null || value.isNull()) throw ParameterErrors.valueInvalid();
        return value;
    }
}
