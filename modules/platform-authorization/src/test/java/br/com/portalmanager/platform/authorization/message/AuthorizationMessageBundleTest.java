package br.com.portalmanager.platform.authorization.message;

import br.com.portalmanager.platform.messaging.message.PlatformDefaultMessageProvider;
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
    void shouldResolveEnglishAuthorizationMessageFromModuleBundle() {
        var message = provider.find(
                AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED,
                Locale.ENGLISH
        ).orElseThrow();

        assertEquals("Access denied", message.message());
        assertEquals("en", message.locale());
    }
}
