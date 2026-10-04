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
    void shouldResolveResourceIdentifierMessageInPortuguese() {
        var message = provider.find(
                AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("AUD-500-004", message.code());
        assertEquals("Não foi possível resolver o identificador do recurso para auditoria.", message.message());
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
        assertEquals("AUD-500-004", provider.find(
                AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING, locale).orElseThrow().code());
        assertEquals("AUD-500-005", provider.find(
                AuditMessageKeys.FIELD_NOT_ALLOWED, locale).orElseThrow().code());
        assertEquals("AUD-500-006", provider.find(
                AuditMessageKeys.RESOURCE_ACTION_REQUIRED, locale).orElseThrow().code());
        assertEquals("AUD-500-007", provider.find(
                AuditMessageKeys.EVENT_TOO_LARGE, locale).orElseThrow().code());
        assertEquals("AUD-500-008", provider.find(
                AuditMessageKeys.EVENT_SERIALIZATION_FAILED, locale).orElseThrow().code());
        assertEquals("AUD-500-009", provider.find(
                AuditMessageKeys.EVENT_COUNT_EXCEEDED, locale).orElseThrow().code());
    }
}
