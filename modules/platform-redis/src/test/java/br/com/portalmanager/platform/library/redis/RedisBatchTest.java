package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.redis.api.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.internal.common.RedisAccess;
import br.com.portalmanager.platform.library.redis.internal.store.DefaultRedisStore;
import br.com.portalmanager.platform.library.redis.internal.serialization.RedisJsonCodec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;

import reactor.core.publisher.Sinks;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

class RedisBatchTest {
    private final PlatformRedisProperties properties =
            new Binder(
                            new MapConfigurationPropertySource(
                                    Map.of()))
                    .bindOrCreate("platform.redis", PlatformRedisProperties.class);
    private final RedisAccess access = new RedisAccess(properties, "unit");
    private final RedisJsonCodec codec = new RedisJsonCodec(new JsonMapper(), 1024);

    @Test
    void deletesInOneCommandWithNamespaceAndValidatesTheWholeBatchBeforeIo() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        DefaultRedisStore store = new DefaultRedisStore(template, codec, access);
        clearInvocations(template);
        assertThat(store.deleteAll(List.of())).isZero();
        verifyNoInteractions(template);
        when(template.delete(List.of("unit:a", "unit:b"))).thenReturn(2L);
        assertThat(store.deleteAll(List.of("a", "b", "a"))).isEqualTo(2L);
        verify(template).delete(List.of("unit:a", "unit:b"));
        clearInvocations(template);
        assertThatThrownBy(() -> store.deleteAll(List.of("a", ""))).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.deleteAll(null)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.deleteAll(Collections.nCopies(1001, "a")))
                .hasMessage("redis.batch.limit");
        verifyNoInteractions(template);
    }

    @Test
    void performsOneMultiCommandAndNoIndividualCommandsAndEmptyBatchesDoNotUseRedis() {
        var template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        var store = new DefaultRedisStore(template, codec, access);
        clearInvocations(template);
        store.setAll(Map.of());
        assertThat(store.getAll(List.of(), String.class)).isEmpty();
        verifyNoInteractions(template, values);
        var entries = new LinkedHashMap<String, String>();
        entries.put("a", "one");
        entries.put("b", "two");
        store.setAll(entries);
        verify(values).multiSet(Map.of("unit:a", "\"one\"", "unit:b", "\"two\""));
        when(values.multiGet(anyCollection()))
                .thenReturn(Arrays.asList("\"one\"", null, "\"two\""));
        assertThat(store.getAll(List.of("a", "missing", "b", "a"), String.class))
                .containsExactly(entry("a", "one"), entry("b", "two"));
        verify(values).multiGet(List.of("unit:a", "unit:missing", "unit:b"));
        verifyNoMoreInteractions(values);
        assertThatThrownBy(() -> store.setAll(null)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.getAll(null, String.class)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.getAll(List.of(), (TypeReference<Object>) null))
                .hasMessage("redis.validation");
    }

    @Test
    void customFactoryWithoutTopologyMetadataConservativelyRequiresOneSlot() {
        var factory = mock(org.springframework.data.redis.connection.RedisConnectionFactory.class);
        var template = mock(StringRedisTemplate.class);
        when(template.getConnectionFactory()).thenReturn(factory);
        var store = new DefaultRedisStore(template, codec, access);
        assertThatThrownBy(() -> store.setAll(Map.of("a", "one", "b", "two")))
                .hasMessage("redis.validation");
        verify(template, never()).opsForValue();
    }
}
