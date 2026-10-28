package br.com.portalmanager.platform.library.redis;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.internal.DefaultRedisStore;
import br.com.portalmanager.platform.library.redis.internal.RedisAccess;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RedisScanTest {
    private final PlatformRedisProperties properties = new Binder(new MapConfigurationPropertySource(
            Map.of("platform.redis.namespace", "test"))).bindOrCreate("platform.redis", PlatformRedisProperties.class);
    private final RedisAccess access = new RedisAccess(properties, "unit");
    private final RedisJsonCodec codec = new RedisJsonCodec(new JsonMapper(), 1024);

    @Test void rejectsInvalidCursorAndLimitsBeforeConnecting() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        DefaultRedisStore store = new DefaultRedisStore(template, codec, access);
        clearInvocations(template);
        for (String cursor : List.of("", "-1", "+1", "01", "a", "18446744073709551616", "1".repeat(100))) {
            assertThatThrownBy(() -> store.scan("service.", cursor, 10)).hasMessage("redis.validation");
        }
        assertThatThrownBy(() -> store.scan("service.", null, 10)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.scan(null, "0", 10)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.scan("service.", "0", 0)).hasMessage("redis.validation");
        assertThatThrownBy(() -> store.scan("service.", "0", 1001)).hasMessage("redis.validation");
        verifyNoInteractions(template);
    }

    @Test void clusterCannotReturnAMisleadingPartialScan() {
        LettuceConnectionFactory factory = mock(LettuceConnectionFactory.class);
        when(factory.isClusterAware()).thenReturn(true);
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        when(template.getConnectionFactory()).thenReturn(factory);
        DefaultRedisStore store = new DefaultRedisStore(template, codec, access);
        clearInvocations(template);
        assertThatThrownBy(() -> store.scan("service.", "0", 10))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException.class);
        verifyNoInteractions(template);
    }

    @Test void continuationPreservesUnsignedCursorAndNeverReturnsForeignPrefixes() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        doReturn(List.of("18446744073709551615", List.of("test.unit:service.a", "foreign:service.b", "test.unit:service-other.c")))
                .when(template).execute(any(RedisScript.class), anyList(), any(Object[].class));
        DefaultRedisStore store = new DefaultRedisStore(template, codec, access);
        br.com.portalmanager.platform.library.redis.core.RedisKeyPage page = store.scan("service.", "0", 1);
        assertThat(page.cursor()).isEqualTo("18446744073709551615");
        assertThat(page.keys()).containsExactly("service.a");
        assertThatThrownBy(() -> page.keys().add("extra")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void emptyPageWithNonzeroCursorDoesNotMeanEndOfTraversal() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        doReturn(List.of("42", List.of())).when(template).execute(any(RedisScript.class), anyList(), any(Object[].class));
        DefaultRedisStore store = new DefaultRedisStore(template, codec, access);
        assertThat(store.scan("service.", "0", 1).cursor()).isEqualTo("42");
    }
}
