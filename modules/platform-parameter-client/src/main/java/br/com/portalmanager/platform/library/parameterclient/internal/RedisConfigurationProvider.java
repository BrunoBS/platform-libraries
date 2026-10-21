package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.parameterclient.ConfigurationProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterFields;
import org.springframework.util.ClassUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import java.util.Objects;

/** Typed, read-only access to CONFIGURATION values. */
public final class RedisConfigurationProvider implements ConfigurationProvider {
    private static final JsonMapper TYPE_MAPPER = JsonMapper.builder().build();
    private final RedisParameterReader reader;

    public RedisConfigurationProvider(RedisParameterReader reader) {
        this.reader = Objects.requireNonNull(reader);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        if (defaultValue == null) throw ParameterErrors.invalid();
        Class<?> type = defaultValue.getClass();
        if (!ParameterFields.INFERABLE.contains(type) && !(defaultValue instanceof Enum<?>)) {
            throw ParameterErrors.invalid();
        }
        if (defaultValue instanceof Enum<?> value) type = value.getDeclaringClass();
        return get(key, (Class<T>) type, defaultValue);
    }

    @Override
    public <T> T get(String key, Class<T> type, T defaultValue) {
        if (type == null) throw ParameterErrors.invalid();
        validateType(TYPE_MAPPER.constructType(type), defaultValue);
        return reader.configuration(key, type, defaultValue);
    }

    @Override
    public <T> T get(String key, TypeReference<T> type, T defaultValue) {
        if (type == null) throw ParameterErrors.invalid();
        validateType(TYPE_MAPPER.constructType(type), defaultValue);
        return reader.configuration(key, type, defaultValue);
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

}
