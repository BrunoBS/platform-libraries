package com.empresa.platform.messaging.message;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiMessageDefinitionParserTest {

    private final ApiMessageDefinitionParser parser = new ApiMessageDefinitionParser();

    @Test
    void shouldParsePipeDelimitedDefinition() {
        var message = parser.parse(
                "validation.id.required",
                Locale.forLanguageTag("pt-BR"),
                "VALIDATION-0002|400|O identificador é obrigatório.|Informe um identificador válido."
        );

        assertThat(message.code()).isEqualTo("VALIDATION-0002");
        assertThat(message.messageKey()).isEqualTo("validation.id.required");
        assertThat(message.locale()).isEqualTo("pt-BR");
        assertThat(message.message()).isEqualTo("O identificador é obrigatório.");
        assertThat(message.solution()).isEqualTo("Informe um identificador válido.");
        assertThat(message.httpStatus()).isEqualTo(400);
    }

    @Test
    void shouldAllowEmptySolution() {
        var message = parser.parse(
                "validation.required",
                Locale.forLanguageTag("pt-BR"),
                "VALIDATION-0001|400|O recurso é obrigatório.|"
        );

        assertThat(message.solution()).isNull();
    }

    @Test
    void shouldRejectMalformedDefinition() {
        assertThatThrownBy(() -> parser.parse(
                "validation.invalid",
                Locale.forLanguageTag("pt-BR"),
                "VALIDATION-0001|400|Mensagem sem solution"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code|httpStatus|message|solution");
    }
}
