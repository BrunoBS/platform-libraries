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
        assertThat(message.orElseThrow().code()).isEqualTo("GLOBAL-0001");
        assertThat(message.orElseThrow().httpStatus()).isEqualTo(400);
        assertThat(message.orElseThrow().message())
                .isEqualTo("Um ou mais campos informados são inválidos. Verifique os detalhes.");
    }

    @Test
    void deveFornecerMensagensBaseDoCrud() {
        var ptBr = provider.find(
                "validation.id.must-be-absent",
                Locale.forLanguageTag("pt-BR")
        );

        var en = provider.find(
                "validation.id.required",
                Locale.forLanguageTag("en-US")
        );

        assertThat(ptBr).isPresent();
        assertThat(ptBr.orElseThrow().code()).isEqualTo("VALIDATION-0003");

        assertThat(en).isPresent();
        assertThat(en.orElseThrow().message()).isEqualTo("The identifier is required.");
        assertThat(en.orElseThrow().httpStatus()).isEqualTo(400);
    }

    @Test
    void deveIgnorarMensagemQueNaoPertenceAoProvider() {
        assertThat(provider.find(
                "account.name.invalid",
                Locale.forLanguageTag("pt-BR")
        )).isEmpty();
    }
}
