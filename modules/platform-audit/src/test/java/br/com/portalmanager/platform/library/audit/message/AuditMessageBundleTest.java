package br.com.portalmanager.platform.library.audit.message;

import br.com.portalmanager.platform.library.messaging.message.PlatformDefaultMessageProvider;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditMessageBundleTest {

    private final PlatformDefaultMessageProvider provider = new PlatformDefaultMessageProvider();

    @Test
    void shouldResolveTransactionRequirementInBothBundles() {
        var portuguese = provider.find(AuditMessageKeys.TRANSACTION_REQUIRED, Locale.forLanguageTag("pt-BR"))
                .orElseThrow();
        var english = provider.find(AuditMessageKeys.TRANSACTION_REQUIRED, Locale.ENGLISH).orElseThrow();

        assertEquals("AUD-500-014", portuguese.code());
        assertEquals("A operação auditável precisa executar dentro de uma transação ativa.", portuguese.message());
        assertEquals("AUD-500-014", english.code());
        assertEquals("The auditable operation must run inside an active transaction.", english.message());
    }

    @Test
    void shouldResolveEveryAuditMessageKeyForPortugueseAndEnglish() {
        var keys = new String[]{
                AuditMessageKeys.USER_CONTEXT_MISSING,
                AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING,
                AuditMessageKeys.EVENT_DEFINITION_REQUIRED,
                AuditMessageKeys.SNAPSHOT_REQUIRED,
                AuditMessageKeys.TRANSACTION_REQUIRED,
                AuditMessageKeys.BEFORE_SNAPSHOT_PROVIDER_REQUIRED,
                AuditMessageKeys.CUSTOM_ACTION_NOT_CONFIGURED,
                AuditMessageKeys.EVENT_SERIALIZATION_FAILED
        };

        for (String key : keys) {
            assertEquals(true, provider.find(key, Locale.forLanguageTag("pt-BR")).isPresent(), key);
            assertEquals(true, provider.find(key, Locale.ENGLISH).isPresent(), key);
        }
    }
}
