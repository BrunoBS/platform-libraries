package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.parameterclient.ParameterProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterFields;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterType;
import br.com.portalmanager.platform.library.parameterclient.internal.expression.ExpressionEvaluator;
import br.com.portalmanager.platform.library.parameterclient.internal.serialization.ExactDocument;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Stateless, read-only adapter. Namespace application belongs exclusively to RedisStore. */
public final class RedisParameterProvider implements ParameterProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RedisParameterProvider.class);
    private static final JsonMapper TYPE_MAPPER = JsonMapper.builder().build();
    private static final Set<Class<?>> INFERABLE = Set.of(
            String.class, Boolean.class, Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class, BigInteger.class, BigDecimal.class);
    private final RedisStore store;
    private final RedisJsonCodec codec;

    public RedisParameterProvider(RedisStore store, RedisJsonCodec codec) {
        this.store = Objects.requireNonNull(store);
        this.codec = Objects.requireNonNull(codec);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        if (defaultValue == null) throw ParameterErrors.invalid();
        Class<?> type = defaultValue.getClass();
        if (!INFERABLE.contains(type) && !(defaultValue instanceof Enum<?>)) {
            throw ParameterErrors.invalid();
        }
        if (defaultValue instanceof Enum<?> value) type = value.getDeclaringClass();
        return get(key, (Class<T>) type, defaultValue);
    }

    @Override
    public <T> T get(String key, Class<T> type, T defaultValue) {
        if (type == null) throw ParameterErrors.invalid();
        validateType(TYPE_MAPPER.constructType(type), defaultValue);
        return read(key, defaultValue, value -> codec.decode(value, type));
    }

    @Override
    public <T> T get(String key, TypeReference<T> type, T defaultValue) {
        if (type == null) throw ParameterErrors.invalid();
        validateType(TYPE_MAPPER.constructType(type), defaultValue);
        return read(key, defaultValue, value -> codec.decode(value, type));
    }

    private void validateType(JavaType type, Object defaultValue) {
        if (!concrete(type) || (defaultValue != null
                && !ClassUtils.resolvePrimitiveIfNecessary(type.getRawClass()).isInstance(defaultValue))) {
            throw ParameterErrors.invalid();
        }
    }

    private boolean concrete(JavaType type) {
        if (type == null || type.hasRawClass(Object.class) || type.hasRawClass(Void.class)
                || type.hasRawClass(void.class)) return false;
        if (type.getRawClass().getTypeParameters().length > type.containedTypeCount()) return false;
        for (int i = 0; i < type.containedTypeCount(); i++) {
            if (!concrete(type.containedType(i))) return false;
        }
        if (type.isArrayType() && !concrete(type.getContentType())) return false;
        return true;
    }

    @Override
    public boolean matches(String key, Object input) {
        validateExpressionArguments(key, input);
        return evaluate(key, input);
    }

    @Override
    public boolean matches(String key, Object input, boolean defaultValue) {
        validateExpressionArguments(key, input);
        try {
            return evaluate(key, input);
        } catch (ApiException failure) {
            LOG.warn("Parameter expression fallback: error={}", failure.getMessageKey());
            return defaultValue;
        }
    }

    private void validateExpressionArguments(String key, Object input) {
        if (input == null) throw ParameterErrors.invalid();
        validateKey(key);
    }

    private void validateKey(String key) {
        // Colon/prefixes, whitespace and controls cannot be logical producer keys.
        if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw ParameterErrors.invalid();
        }
    }

    private boolean evaluate(String key, Object input) {
        ExactDocument document;
        try {
            document = store.get(key, ExactDocument.class).orElseThrow(ParameterErrors::missing);
        } catch (ApiException failure) {
            if (failure.getMessageKey().startsWith("parameter-client.")) throw failure;
            throw ParameterErrors.readFailure(failure);
        }
        return ExpressionEvaluator.evaluate(document.root(), input);
    }

    private <T> T read(String key, T defaultValue, Function<String, T> convert) {
        validateKey(key);
        Optional<JsonNode> envelope = readEnvelope(key);
        if (envelope.isEmpty()) return defaultValue;

        JsonNode value = publishedValue(envelope.get());
        try {
            return convert.apply(value.toString());
        } catch (RuntimeException failure) {
            // RedisJsonCodec is strict; do not retain payload-bearing conversion exceptions.
            throw ParameterErrors.valueInvalid();
        }
    }

    private Optional<JsonNode> readEnvelope(String key) {
        try {
            return store.get(key, JsonNode.class);
        } catch (RuntimeException failure) {
            throw ParameterErrors.readFailure(failure);
        }
    }

    private JsonNode publishedValue(JsonNode envelope) {
        if (!envelope.isObject()) throw ParameterErrors.valueInvalid();

        JsonNode type = envelope.get(ParameterFields.TYPE);
        if (type == null || !type.isString()
                || !ParameterType.isSupported(type.asString())) {
            throw ParameterErrors.valueInvalid();
        }

        JsonNode value = envelope.get(ParameterFields.VALUE);
        if (value == null || value.isNull()) throw ParameterErrors.valueInvalid();
        return value;
    }
}
