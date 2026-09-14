package com.empresa.platform.authorization.message;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationMessageProviderTest {

    private final AuthorizationMessageProvider provider = new AuthorizationMessageProvider();

    @Test
    void shouldReturnPortugueseFallbackForPlatformAccessDenied() {
        var message = provider.find(
                AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("Acesso não permitido", message.message());
        assertEquals(401, message.httpStatus());
        assertEquals("pt-BR", message.locale());
    }

    @Test
    void shouldReturnEnglishFallbackWhenRequested() {
        var message = provider.find(
                AuthorizationMessageKeys.PLATFORM_ACCESS_DENIED,
                Locale.forLanguageTag("en-US")
        ).orElseThrow();

        assertEquals("Access denied", message.message());
        assertEquals(401, message.httpStatus());
        assertEquals("en", message.locale());
    }

    @Test
    void shouldReturnEmptyForUnknownKey() {
        assertTrue(provider.find("authorization.unknown", Locale.forLanguageTag("pt-BR")).isEmpty());
    }
}
