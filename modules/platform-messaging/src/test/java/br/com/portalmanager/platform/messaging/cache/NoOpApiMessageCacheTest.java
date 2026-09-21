package br.com.portalmanager.platform.messaging.cache;

import br.com.portalmanager.platform.messaging.model.ApiMessage;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoOpApiMessageCacheTest {

    private final NoOpApiMessageCache cache = new NoOpApiMessageCache();

    @Test
    void shouldAlwaysMiss() {
        // Garante que qualquer busca por chave e locale sempre retornará um Optional vazio
        assertTrue(cache.get("user.not.found", Locale.US).isEmpty());
        assertTrue(cache.get("order.expired", Locale.forLanguageTag("pt-BR")).isEmpty());
    }

    @Test
    void shouldDoNothingOnPutWithoutThrowingException() {
        ApiMessage message = new ApiMessage(
                "ERR-404",
                "user.not.found",
                "pt-BR",
                "Usuário não encontrado",
                "Verifique o ID",
                404
        );

        // Garante que salvar dados no cache desativado é uma operação segura que não quebra a aplicação
        assertDoesNotThrow(() -> cache.put(message));
    }
}
