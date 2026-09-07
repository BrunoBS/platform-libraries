package com.empresa.platform.messaging.resolver;

import com.empresa.platform.messaging.cache.NoOpApiMessageCache;
import com.empresa.platform.messaging.exception.ApiMessageNotFoundException;
import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.repository.ApiMessageRepository;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultApiMessageResolverTest {

    @Test
    void shouldUseLanguageFallback() {
        // Simula o banco contendo apenas o idioma puro 'en'
        ApiMessageRepository r = (k, l) -> l.equals(Locale.ENGLISH)
                ? Optional.of(new ApiMessage("ERR-0001", k, "en", "User not found.", "Check the identifier.", 404))
                : Optional.empty();

        var resolver = new DefaultApiMessageResolver(r, new NoOpApiMessageCache(), Locale.forLanguageTag("pt-BR"));

        // Pede 'en-US' e deve encontrar 'en'
        var result = resolver.resolve("user.not.found", Locale.forLanguageTag("en-US"));

        assertEquals("en", result.locale());
        assertEquals(404, result.httpStatus());
    }

    @Test
    void shouldFallBackToAbsoluteDefaultWhenLanguageDoesNotExist() {
        ApiMessageRepository r = (k, l) -> l.equals(Locale.forLanguageTag("pt-BR"))
                ? Optional.of(new ApiMessage("ERR-0001", k, "pt-BR", "Usuário não encontrado.", "Verifique o identificador.", 404))
                : Optional.empty();

        var resolver = new DefaultApiMessageResolver(r, new NoOpApiMessageCache(), Locale.forLanguageTag("en"));
        var result = resolver.resolve("user.not.found", Locale.forLanguageTag("fr-FR"));

        assertEquals("pt-BR", result.locale());
        assertEquals("Usuário não encontrado.", result.message());
    }

    @Test
    void shouldThrowExceptionWhenMessageIsNotFoundInAnyCandidate() {
        ApiMessageRepository r = (k, l) -> Optional.empty();
        var resolver = new DefaultApiMessageResolver(r, new NoOpApiMessageCache(), Locale.forLanguageTag("pt-BR"));
        assertThrows(ApiMessageNotFoundException.class, () ->
                resolver.resolve("key.inexistente", Locale.forLanguageTag("en-US"))
        );
    }
}
