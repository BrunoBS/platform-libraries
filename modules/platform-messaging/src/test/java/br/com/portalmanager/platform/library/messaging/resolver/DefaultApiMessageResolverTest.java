package br.com.portalmanager.platform.library.messaging.resolver;

import br.com.portalmanager.platform.library.messaging.cache.NoOpApiMessageCache;
import br.com.portalmanager.platform.library.messaging.exception.ApiMessageNotFoundException;
import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.provider.ApiMessageProvider;
import br.com.portalmanager.platform.library.messaging.repository.ApiMessageRepository;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultApiMessageResolverTest {

    @Test
    void shouldUseLanguageFallback() {
        ApiMessageRepository repository = (k, l) -> l.equals(Locale.ENGLISH)
                ? Optional.of(new ApiMessage("ERR-0001", k, "en", "User not found.", "Check the identifier.", 404))
                : Optional.empty();

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                new PlatformDefaultMessageProvider()
        );

        var result = resolver.resolve("user.not.found", Locale.forLanguageTag("en-US"));

        assertEquals("en", result.locale());
        assertEquals(404, result.httpStatus());
    }

    @Test
    void shouldUseProviderWhenRepositoryDoesNotContainMessage() {
        ApiMessageRepository repository = (k, l) -> Optional.empty();
        ApiMessageProvider provider = (k, l) -> Optional.of(
                new ApiMessage("AUTH-401", k, l.toLanguageTag(), "Acesso não permitido", "Verifique suas credenciais.", 401)
        );

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                provider
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
                new ApiMessage("AUTH-401", k, l.toLanguageTag(), "Acesso não permitido", "Verifique suas credenciais.", 401)
        );

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                provider
        );

        var result = resolver.resolve("authorization.platform.access.denied", Locale.forLanguageTag("pt-BR"));

        assertEquals("Mensagem customizada", result.message());
        assertEquals("CUSTOM-401", result.code());
    }

    @Test
    void shouldPreferRepositoryLanguageFallbackOverProvider() {
        ApiMessageRepository repository = (k, l) -> l.equals(Locale.ENGLISH)
                ? Optional.of(new ApiMessage("CUSTOM-404", k, "en", "Database override", "Database solution", 404))
                : Optional.empty();

        ApiMessageProvider provider = (k, l) -> Optional.of(
                new ApiMessage("DEFAULT-404", k, l.toLanguageTag(), "Properties default", "Properties solution", 404)
        );

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                provider
        );

        var result = resolver.resolve("user.not.found", Locale.forLanguageTag("en-US"));

        assertEquals("Database override", result.message());
        assertEquals("en", result.locale());
    }

    @Test
    void shouldUseConfiguredDefaultLocaleWhenRequestedPropertiesLocaleDoesNotExist() {
        ApiMessageRepository repository = (k, l) -> Optional.empty();

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                new PlatformDefaultMessageProvider()
        );

        var result = resolver.resolve(PlatformMessageKeys.RESOURCE_NOT_FOUND, Locale.forLanguageTag("fr-FR"));

        assertEquals("pt-BR", result.locale());
        assertEquals("O recurso solicitado não foi encontrado.", result.message());
    }

    @Test
    void shouldThrowExceptionWhenMessageIsNotFoundInAnyCandidate() {
        ApiMessageRepository repository = (k, l) -> Optional.empty();
        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                new PlatformDefaultMessageProvider()
        );

        assertThrows(ApiMessageNotFoundException.class, () ->
                resolver.resolve("key.inexistente", Locale.forLanguageTag("en-US"))
        );
    }
    @Test
    void shouldResolveLocalServiceKeyUsingSpringApplicationNameNamespace() {
        ApiMessageRepository repository = (k, l) ->
                "workspace-service.workspace.not-found".equals(k)
                        ? Optional.of(new ApiMessage(
                                "WORKSPACE-0001",
                                k,
                                l.toLanguageTag(),
                                "Workspace não encontrado.",
                                "Verifique o identificador.",
                                404
                        ))
                        : Optional.empty();

        var resolver = new DefaultApiMessageResolver(
                repository,
                new NoOpApiMessageCache(),
                Locale.forLanguageTag("pt-BR"),
                null,
                "workspace-service"
        );

        var result = resolver.resolve("workspace.not-found", Locale.forLanguageTag("pt-BR"));

        assertEquals("workspace-service.workspace.not-found", result.messageKey());
        assertEquals("WORKSPACE-0001", result.code());
    }
}
