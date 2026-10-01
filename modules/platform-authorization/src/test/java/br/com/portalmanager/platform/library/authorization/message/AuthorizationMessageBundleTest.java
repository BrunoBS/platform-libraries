package br.com.portalmanager.platform.library.authorization.message;

import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationMessageBundleTest {

    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();

    @Test
    void shouldResolveAuthorizationMessageFromModuleBundle() {
        var message = provider.find(
                AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("AUTH-401-004", message.code());
        assertEquals("Acesso não permitido", message.message());
        assertEquals(401, message.httpStatus());
        assertEquals("pt-BR", message.locale());
    }

    @Test
    void shouldResolveServiceUnavailableWithStandardizedContract() {
        var message = provider.find(
                AuthorizationMessageKeys.SERVICE_UNAVAILABLE,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("PLT-AUTH-002", message.code());
        assertEquals("Authorization API indisponível.", message.message());
        assertEquals(503, message.httpStatus());
        assertEquals("pt-BR", message.locale());
    }

    @Test
    void shouldResolveRemainingTechnicalErrorsWithStandardizedContract() {
        var contractError = provider.find(
                AuthorizationMessageKeys.SERVICE_CONTRACT_ERROR,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();
        var groupLimitError = provider.find(
                AuthorizationMessageKeys.TOO_MANY_AUTHORIZER_GROUPS,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("PLT-AUTH-003", contractError.code());
        assertEquals(502, contractError.httpStatus());
        assertEquals("PLT-AUTH-004", groupLimitError.code());
        assertEquals(500, groupLimitError.httpStatus());
    }

    @Test
    void shouldResolveEnglishAuthorizationMessageFromModuleBundle() {
        var message = provider.find(
                AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED,
                Locale.ENGLISH
        ).orElseThrow();

        assertEquals("Access denied", message.message());
        assertEquals("en", message.locale());
    }
}
