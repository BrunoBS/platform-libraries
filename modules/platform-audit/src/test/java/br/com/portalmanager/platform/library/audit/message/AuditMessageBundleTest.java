package br.com.portalmanager.platform.library.audit.message;

import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditMessageBundleTest {

    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();

    @Test
    void shouldResolveDestinationMessageFromPortugueseBundle() {
        var message = provider.find(
                AuditMessageKeys.MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("AUD-500-002", message.code());
        assertEquals("O destino de auditoria precisa estar configurado para publicação ordenada.", message.message());
        assertEquals(500, message.httpStatus());
        assertEquals("pt-BR", message.locale());
    }

    @Test
    void shouldResolveDestinationMessageFromEnglishBundle() {
        var message = provider.find(
                AuditMessageKeys.MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED,
                Locale.ENGLISH
        ).orElseThrow();

        assertEquals("AUD-500-002", message.code());
        assertEquals("The audit destination must be configured for ordered publishing.", message.message());
        assertEquals(500, message.httpStatus());
        assertEquals("en", message.locale());
    }

    @Test
    void shouldResolveAllAuditMessageKeys() {
        var locale = Locale.forLanguageTag("pt-BR");

        assertEquals("AUD-500-001", provider.find(
                AuditMessageKeys.MESSAGE_QUEUE_DESTINATION_REQUIRED, locale).orElseThrow().code());
        assertEquals("AUD-500-002", provider.find(
                AuditMessageKeys.MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED, locale).orElseThrow().code());
        assertEquals("AUD-500-003", provider.find(
                AuditMessageKeys.USER_CONTEXT_MISSING, locale).orElseThrow().code());
    }
}
