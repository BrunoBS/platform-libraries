package br.com.portalmanager.platform.catalog.message;

import br.com.portalmanager.platform.messaging.message.PlatformDefaultMessageProvider;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogMessageBundleTest {

    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();

    @Test
    void shouldResolveCatalogMessageFromModuleBundle() {
        var message = provider.find(
                CatalogMessageKeys.NOT_FOUND,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("CAT-404-001", message.code());
        assertEquals("Catálogo não encontrado.", message.message());
        assertEquals(404, message.httpStatus());
        assertEquals("pt-BR", message.locale());
    }

    @Test
    void shouldResolveEnglishCatalogMessageFromModuleBundle() {
        var message = provider.find(
                CatalogMessageKeys.NOT_FOUND,
                Locale.ENGLISH
        ).orElseThrow();

        assertEquals("Catalog was not found.", message.message());
        assertEquals("en", message.locale());
    }
}
