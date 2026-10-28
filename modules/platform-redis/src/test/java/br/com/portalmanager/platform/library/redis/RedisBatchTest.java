package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.internal.*;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

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
                                    Map.of("platform.redis.namespace", "test")))
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
        when(template.delete(List.of("test.unit:a", "test.unit:b"))).thenReturn(2L);
        assertThat(store.deleteAll(List.of("a", "b", "a"))).isEqualTo(2L);
        verify(template).delete(List.of("test.unit:a", "test.unit:b"));
        clearInvocations(template);
        assertThatThrownBy(() -> store.deleteAll(List.of("a", ""))).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.deleteAll(null)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.deleteAll(Collections.nCopies(1001, "a"))).hasMessage("redis.validation");
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
        verify(values).multiSet(Map.of("test.unit:a", "\"one\"", "test.unit:b", "\"two\""));
        when(values.multiGet(anyCollection()))
                .thenReturn(Arrays.asList("\"one\"", null, "\"two\""));
        assertThat(store.getAll(List.of("a", "missing", "b", "a"), String.class))
                .containsExactly(entry("a", "one"), entry("b", "two"));
        verify(values).multiGet(List.of("test.unit:a", "test.unit:missing", "test.unit:b"));
        verifyNoMoreInteractions(values);
        assertThatThrownBy(() -> store.setAll(null)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.getAll(null, String.class)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.getAll(List.of(), (TypeReference<Object>) null))
                .hasMessage("redis.validation");
    }

    @Test
    void asyncBatchUsesReactiveIoAndReturnsBeforeTheResponseWithoutBlocking() {
        var template = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        var store = new DefaultAsyncRedisStore(template, codec, access);
        var written = Sinks.<Boolean>one();
        when(values.multiSet(anyMap())).thenReturn(written.asMono());
        var write = store.setAll(Map.of("a", "value")).toCompletableFuture();
        assertThat(write).isNotDone();
        written.tryEmitValue(true);
        write.join();
        var read = Sinks.<List<String>>one();
        when(values.multiGet(anyCollection())).thenReturn(read.asMono());
        var result = store.getAll(List.of("missing", "a"), String.class).toCompletableFuture();
        assertThat(result).isNotDone();
        read.tryEmitValue(Arrays.asList(null, "\"value\""));
        assertThat(result.join()).containsExactly(entry("a", "value"));
        verify(values).multiSet(Map.of("test.unit:a", "\"value\""));
        verify(values).multiGet(List.of("test.unit:missing", "test.unit:a"));
        verifyNoMoreInteractions(values);
        assertThatThrownBy(
                        () ->
                                store.getAll(List.of(), (Class<Object>) null)
                                        .toCompletableFuture()
                                        .join())
                .hasCauseInstanceOf(
                        br.com.portalmanager.platform.library.messaging.exception
                                .ValidationException.class);
    }

    @Test
    void clusterRejectsCrossSlotBeforeCommandsAndAcceptsCommonHashTags() {
        var factory = mock(LettuceConnectionFactory.class);
        when(factory.isClusterAware()).thenReturn(true);
        var template = mock(StringRedisTemplate.class);
        when(template.getConnectionFactory()).thenReturn(factory);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(values);
        var store = new DefaultRedisStore(template, codec, access);
        assertThatThrownBy(() -> store.setAll(Map.of("a", "1", "b", "2")))
                .hasMessage("redis.validation");
        assertThatThrownBy(() -> store.getAll(List.of("a", "b"), String.class))
                .hasMessage("redis.validation");
        verifyNoInteractions(values);
        store.setAll(Map.of("{scope}:a", "1", "{scope}:b", "2"));
        verify(values).multiSet(anyMap());
        var reactive = mock(ReactiveStringRedisTemplate.class);
        when(reactive.getConnectionFactory()).thenReturn(factory);
        var async = new DefaultAsyncRedisStore(reactive, codec, access);
        assertThatThrownBy(
                        () -> async.setAll(Map.of("a", "1", "b", "2")).toCompletableFuture().join())
                .hasCauseInstanceOf(
                        br.com.portalmanager.platform.library.messaging.exception
                                .ValidationException.class);
        verify(reactive, never()).opsForValue();
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
