package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.internal.*;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import reactor.core.publisher.Sinks;

import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.*;

class RedisAsyncTest {
    @Test
    void returnsIncompleteStageWithoutBlockingAndCompletesOnReactiveIo() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        var response = Sinks.<String>one();
        var properties =
                new PlatformRedisProperties(
                        "",
                        PlatformRedisProperties.NamespaceMode.ISOLATED,
                        256,
                        1024,
                        10,
                        10,
                        1,
                        2,
                        2,
                        100,
                        Duration.ofSeconds(1),
                        Duration.ofMillis(10),
                        Duration.ofSeconds(1),
                        false);
        when(redis.execute(eq(RedisReadScripts.GET), anyList(), anyList()))
                .thenReturn(response.asMono().flux());
        var async =
                new DefaultAsyncRedisStore(
                        redis,
                        new RedisJsonCodec(new JsonMapper(), 1024),
                        new RedisAccess(properties, "async"));
        var stage = async.get("key", String.class).toCompletableFuture();
        assertThat(stage).isNotDone();
        response.tryEmitValue("\"result\"");
        assertThat(stage.join()).contains("result");
        verify(redis)
                .execute(eq(RedisReadScripts.GET), eq(List.of("async:key")), eq(List.of("1024")));
    }
}
