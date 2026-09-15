package com.empresa.platform.messaging.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlatformDefaultMessageProvider")
class PlatformDefaultMessageProviderTest {

    private final PlatformDefaultMessageProvider provider =
            new PlatformDefaultMessageProvider();

    @Test
    void deveFornecerMensagensGlobaisSemBanco() {
        var message = provider.find(
                PlatformMessageKeys.VALIDATION_FAILED,
                Locale.forLanguageTag("pt-BR")
        );

        assertThat(message).isPresent();
        assertThat(message.orElseThrow().httpStatus()).isEqualTo(400);
    }

    @Test
    void deveFornecerMensagensBaseDoCrud() {
        assertThat(provider.find(
                "validation.id.must-be-absent",
                Locale.forLanguageTag("pt-BR")
        )).isPresent();

        assertThat(provider.find(
                "validation.id.required",
                Locale.forLanguageTag("en-US")
        )).isPresent();
    }

    @Test
    void deveIgnorarMensagemQueNaoPertenceAoProvider() {
        assertThat(provider.find(
                "account.name.invalid",
                Locale.forLanguageTag("pt-BR")
        )).isEmpty();
    }
}
