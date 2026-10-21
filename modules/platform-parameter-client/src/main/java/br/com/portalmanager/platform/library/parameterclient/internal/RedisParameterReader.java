package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterFields;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterType;
import br.com.portalmanager.platform.library.parameterclient.internal.serialization.ExactDocument;
import br.com.portalmanager.platform.library.parameterclient.message.ParameterClientMessageKeys;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

/** Shared Redis read/decoding boundary; providers never expose a RedisStore. */
public final class RedisParameterReader {
    private final RedisStore store;
    private final RedisJsonCodec codec;

    public RedisParameterReader(RedisStore store, RedisJsonCodec codec) {
        this.store = Objects.requireNonNull(store);
        this.codec = Objects.requireNonNull(codec);
    }

    public static void validateKey(String key) {
        if (key == null || key.isBlank()) throw ParameterErrors.invalid();
    }

    public <T> T configuration(String key, Class<T> type, T defaultValue) {
        return configuration(key, defaultValue, json -> codec.decode(json, type));
    }

    public <T> T configuration(String key, TypeReference<T> type, T defaultValue) {
        return configuration(key, defaultValue, json -> codec.decode(json, type));
    }

    private <T> T configuration(String key, T defaultValue, Function<String, T> convert) {
        validateKey(key);
        Optional<JsonNode> envelope;
        try {
            envelope = read(key, JsonNode.class);
        } catch (RuntimeException failure) {
            // Keep CONFIGURATION's existing controlled read-failure semantics.
            if (failure instanceof ApiException api
                    && api.getMessageKey().startsWith(ParameterClientMessageKeys.PREFIX)) throw api;
            throw ParameterErrors.readFailure(failure);
        }
        if (envelope.isEmpty()) return defaultValue;
        JsonNode value = configurationValue(envelope.get());
        try {
            return convert.apply(value.toString());
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
            return store.get(key, type);
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
