package com.empresa.platform.messaging.resolver;

import com.empresa.platform.messaging.cache.NoOpApiMessageCache;
import com.empresa.platform.messaging.exception.ApiMessageNotFoundException;
import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.provider.ApiMessageProvider;
import com.empresa.platform.messaging.repository.ApiMessageRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultApiMessageResolverTest {

    @Test
    void shouldUseLanguageFallback() {
        ApiMessageRepository r = (k, l) -> l.equals(Locale.ENGLISH)
                ? Optional.of(new ApiMessage("ERR-0001", k, "en", "User not found.", "Check the identifier.", 404))
                : Optional.empty();

        var resolver = new DefaultApiMessageResolver(r, new NoOpApiMessageCache(), Locale.forLanguageTag("pt-BR"));
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
    void shouldUseProviderWhenRepositoryDoesNotContainMessage() {
        ApiMessageRepository repository = (k, l) -> Optional.empty();
        ApiMessageProvider provider = (k, l) -> Optional.of(
                new ApiMessage("AUTH-401", k, "pt-BR", "Acesso não permitido", "Verifique suas credenciais.", 401)
        );

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                List.of(provider)
        );

        var result = resolver.resolve("authorization.platform.access.denied", Locale.forLanguageTag("pt-BR"));

        assertEquals("Acesso não permitido", result.message());
        assertEquals(401, result.httpStatus());
    }

    @Test
    void shouldPreferRepositoryOverProvider() {
        ApiMessageRepository repository = (k, l) -> Optional.of(
                new ApiMessage("CUSTOM-401", k, l.toLanguageTag(), "Mensagem customizada", "Custom solution", 401)
        );
        ApiMessageProvider provider = (k, l) -> Optional.of(
                new ApiMessage("AUTH-401", k, "pt-BR", "Acesso não permitido", "Verifique suas credenciais.", 401)
        );

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                List.of(provider)
        );

        var result = resolver.resolve("authorization.platform.access.denied", Locale.forLanguageTag("pt-BR"));

        assertEquals("Mensagem customizada", result.message());
        assertEquals("CUSTOM-401", result.code());
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
