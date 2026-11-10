package br.com.portalmanager.platform.library.messaging.cache;

import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.redis.api.store.RedisStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisApiMessageCacheTest {

    private RedisStore redisStore;
    private RedisApiMessageCache cache;
    private final Duration ttl = Duration.ofHours(1);

    @BeforeEach
    void setUp() {
        redisStore = mock(RedisStore.class);
        cache = new RedisApiMessageCache(redisStore, ttl);
    }

    @Test
    void shouldReturnApiMessageWhenPresentInCache() {
        String key = "user.not.found";
        Locale locale = Locale.of("pt", "BR");
        String cacheKey = "platform:message:user.not.found:pt-BR";
        ApiMessage expected = new ApiMessage(
                "ERR-404", key, "pt-BR", "Usuário não encontrado.", "Verifique o ID.", 404);

        when(redisStore.get(cacheKey, ApiMessage.class)).thenReturn(Optional.of(expected));

        Optional<ApiMessage> result = cache.get(key, locale);

        assertEquals(Optional.of(expected), result);
    }

    @Test
    void shouldReturnEmptyOptionalWhenCacheIsMiss() {
        String key = "unknown.key";
        Locale locale = Locale.US;
        String cacheKey = "platform:message:unknown.key:en-US";

        when(redisStore.get(cacheKey, ApiMessage.class)).thenReturn(Optional.empty());

        assertTrue(cache.get(key, locale).isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionalWhenRedisStoreFails() {
        String key = "timeout.key";
        Locale locale = Locale.US;
        String cacheKey = "platform:message:timeout.key:en-US";

        when(redisStore.get(cacheKey, ApiMessage.class))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        assertTrue(cache.get(key, locale).isEmpty());
    }

    @Test
    void shouldSaveMessageInCacheWithConfiguredTtl() {
        ApiMessage message = new ApiMessage(
                "ERR-404", "user.not.found", "pt-BR", "Usuário não encontrado.", "Verifique o ID.", 404);
        String expectedCacheKey = "platform:message:user.not.found:pt-BR";

        cache.put(message);

        verify(redisStore).set(eq(expectedCacheKey), eq(message), eq(ttl));
    }

    @Test
    void shouldDeleteKeyFromRedisStoreWhenEvictIsCalled() {
        String key = "user.not.found";
        Locale locale = Locale.of("pt", "BR");
        String expectedCacheKey = "platform:message:user.not.found:pt-BR";

        cache.evict(key, locale);

        verify(redisStore).delete(eq(expectedCacheKey));
    }
}
