package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.parameterclient.ParameterProvider;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.util.ClassUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Stateless, read-only adapter. Namespace application belongs exclusively to RedisStore. */
public final class RedisParameterProvider implements ParameterProvider {
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

    private <T> T read(String key, T defaultValue, Function<String, T> convert) {
        // Colon/prefixes, whitespace and controls cannot be logical producer keys.
        if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw ParameterErrors.invalid();
        }
        JsonNode envelope;
        try {
            var found = store.get(key, JsonNode.class);
            if (found.isEmpty()) return defaultValue;
            envelope = found.get();
        } catch (RuntimeException failure) {
            throw ParameterErrors.readFailure(failure);
        }
        JsonNode type = envelope.get("type");
        JsonNode value = envelope.get("value");
        if (!envelope.isObject() || type == null || !type.isString()
                || !("CONFIGURATION".equals(type.asString()) || "EXPRESSION".equals(type.asString()))
                || value == null || value.isNull()) {
            throw ParameterErrors.valueInvalid();
        }
        try {
            return convert.apply(value.toString());
        } catch (RuntimeException failure) {
            // RedisJsonCodec is strict; do not retain payload-bearing conversion exceptions.
            throw ParameterErrors.valueInvalid();
        }
    }
}
