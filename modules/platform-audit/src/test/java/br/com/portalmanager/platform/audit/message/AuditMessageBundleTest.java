package br.com.portalmanager.platform.audit.message;

import br.com.portalmanager.platform.messaging.message.PlatformDefaultMessageProvider;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditMessageBundleTest {

    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();

    @Test
    void shouldResolveAuditMessageFromPortugueseBundle() {
        var message = provider.find(
                AuditMessageKeys.SERVICE_URL_REQUIRED,
                Locale.forLanguageTag("pt-BR")
        ).orElseThrow();

        assertEquals("AUD-500-001", message.code());
        assertEquals("URL do serviço de auditoria não configurada.", message.message());
        assertEquals(500, message.httpStatus());
        assertEquals("pt-BR", message.locale());
    }

    @Test
    void shouldResolveAuditMessageFromEnglishBundle() {
        var message = provider.find(
                AuditMessageKeys.SERVICE_URL_REQUIRED,
                Locale.ENGLISH
        ).orElseThrow();

        assertEquals("AUD-500-001", message.code());
        assertEquals("Audit service URL is not configured.", message.message());
        assertEquals(500, message.httpStatus());
        assertEquals("en", message.locale());
    }

    @Test
    void shouldResolveAllAuditMessageKeys() {
        var locale = Locale.forLanguageTag("pt-BR");

        assertEquals("AUD-500-001", provider.find(AuditMessageKeys.SERVICE_URL_REQUIRED, locale).orElseThrow().code());
        assertEquals("AUD-500-002", provider.find(AuditMessageKeys.USER_CONTEXT_MISSING, locale).orElseThrow().code());
        assertEquals("AUD-500-003", provider.find(AuditMessageKeys.FALLBACK_STORE_MISSING, locale).orElseThrow().code());
        assertEquals("AUD-500-004", provider.find(AuditMessageKeys.REDIS_NOT_CONFIGURED, locale).orElseThrow().code());
        assertEquals("AUD-500-005", provider.find(AuditMessageKeys.FALLBACK_PERSIST_FAILED, locale).orElseThrow().code());
        assertEquals("AUD-500-006", provider.find(AuditMessageKeys.FALLBACK_DESERIALIZE_FAILED, locale).orElseThrow().code());
    }
}
