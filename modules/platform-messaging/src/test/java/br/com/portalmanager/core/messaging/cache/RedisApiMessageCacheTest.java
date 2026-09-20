package br.com.portalmanager.core.messaging.cache;

import br.com.portalmanager.core.messaging.model.ApiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RedisApiMessageCacheTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisApiMessageCache cache;
    private final Duration ttl = Duration.ofHours(1);
    private final String delimiter = "\u0000";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache = new RedisApiMessageCache(redisTemplate, ttl);
    }

    @Test
    void shouldReturnApiMessageWhenPresentInCache() {
        // Arrange
        String key = "user.not.found";
        Locale locale = Locale.of("pt", "BR");
        String cacheKey = "platform:message:user.not.found:pt-BR";

        // Monta a string no formato exato esperado pela classe real usando o delimitador nulo
        String rawValue = String.join(delimiter,
                "ERR-404", "user.not.found", "pt-BR", "Usuário não encontrado.", "Verifique o ID.", "404");

        when(valueOperations.get(cacheKey)).thenReturn(rawValue);

        // Act
        Optional<ApiMessage> result = cache.get(key, locale);

        // Assert
        assertTrue(result.isPresent());
        ApiMessage message = result.get();
        assertEquals("ERR-404", message.code());
        assertEquals("user.not.found", message.messageKey());
        assertEquals("pt-BR", message.locale());
        assertEquals("Usuário não encontrado.", message.message());
        assertEquals("Verifique o ID.", message.solution());
        assertEquals(404, message.httpStatus());
    }

    @Test
    void shouldReturnEmptyOptionalWhenCacheIsMiss() {
        // Arrange
        String key = "unknown.key";
        Locale locale = Locale.US;
        String cacheKey = "platform:message:unknown.key:en-US";

        when(valueOperations.get(cacheKey)).thenReturn(null);

        // Act
        Optional<ApiMessage> result = cache.get(key, locale);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionalWhenPayloadIsCorrupted() {
        // Arrange
        String key = "invalid.key";
        Locale locale = Locale.US;
        String cacheKey = "platform:message:invalid.key:en-US";

        // Payload inválido com menos de 6 partes separadas
        String corruptedValue = "ERR-500" + delimiter + "invalid.key" + delimiter + "en-US";

        when(valueOperations.get(cacheKey)).thenReturn(corruptedValue);

        // Act
        Optional<ApiMessage> result = cache.get(key, locale);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyOptionalWhenRedisThrowsException() {
        // Arrange
        String key = "timeout.key";
        Locale locale = Locale.US;
        String cacheKey = "platform:message:timeout.key:en-US";

        // Simula uma queda ou timeout de conexão do Redis
        when(valueOperations.get(cacheKey)).thenThrow(new RuntimeException("Redis connection refused"));

        // Act
        Optional<ApiMessage> result = cache.get(key, locale);

        // Assert
        assertTrue(result.isEmpty()); // Deve engolir o erro e retornar vazio de forma segura
    }

    @Test
    void shouldSaveMessageInCacheWithCorrectStructureAndTtl() {
        // Arrange
        ApiMessage message = new ApiMessage(
                "ERR-404", "user.not.found", "pt-BR", "Usuário não encontrado.", "Verifique o ID.", 404);
        String expectedCacheKey = "platform:message:user.not.found:pt-BR";

        String expectedRawValue = String.join(delimiter,
                "ERR-404", "user.not.found", "pt-BR", "Usuário não encontrado.", "Verifique o ID.", "404");

        // Act
        cache.put(message);

        // Assert
        verify(valueOperations, times(1)).set(eq(expectedCacheKey), eq(expectedRawValue), eq(ttl));
    }

    @Test
    void shouldDeleteKeyFromRedisWhenEvictIsCalled() {
        // Arrange
        String key = "user.not.found";
        Locale locale = Locale.of("pt", "BR");
        String expectedCacheKey = "platform:message:user.not.found:pt-BR";

        // Act
        cache.evict(key, locale);

        // Assert
        verify(redisTemplate, times(1)).delete(eq(expectedCacheKey));
    }
}
