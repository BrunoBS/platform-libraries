package br.com.portalmanager.platform.library.messaging.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformMessagingPropertiesTest {

    @Test
    void shouldInitializeWithDefaultValues() {
        PlatformMessagingProperties properties = new PlatformMessagingProperties();

        assertTrue(properties.isEnabled());
        assertEquals("pt-BR", properties.getDefaultLocale());
        assertFalse(properties.getDatasource().isEnabled());
        assertEquals("vw_api_message", properties.getDatasource().getViewName());
        assertFalse(properties.getCache().isEnabled());
        assertEquals(Duration.ofHours(1), properties.getCache().getTtl());
        assertEquals(Locale.forLanguageTag("pt-BR"), properties.resolveDefaultLocale());
    }

    @Test
    void shouldFallbackToSafeLocaleWhenConfiguredLocaleIsInvalid() {
        PlatformMessagingProperties properties = new PlatformMessagingProperties();

        properties.setDefaultLocale("   ");

        assertEquals(Locale.forLanguageTag("pt-BR"), properties.resolveDefaultLocale());
    }

    @Test
    void shouldResolveConfiguredLocale() {
        PlatformMessagingProperties properties = new PlatformMessagingProperties();

        properties.setDefaultLocale("en-US");

        assertEquals(Locale.forLanguageTag("en-US"), properties.resolveDefaultLocale());
    }
}
