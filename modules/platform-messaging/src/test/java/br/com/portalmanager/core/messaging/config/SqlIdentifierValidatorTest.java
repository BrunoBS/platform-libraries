package br.com.portalmanager.core.messaging.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlIdentifierValidatorTest {

    @Test
    void shouldAllowValidIdentifiers() {
        assertEquals("vw_api_message", SqlIdentifierValidator.validate("vw_api_message"));
        assertEquals("tabela123_messages", SqlIdentifierValidator.validate("tabela123_messages"));
    }

    @Test
    void shouldThrowExceptionForInvalidIdentifiers() {
        // Testando tentativas de injeção sql e caracteres proibidos
        assertThrows(IllegalArgumentException.class, () -> SqlIdentifierValidator.validate("vw_api_message; DROP TABLE x;"));
        assertThrows(IllegalArgumentException.class, () -> SqlIdentifierValidator.validate("vw-api-message")); // hífen
        assertThrows(IllegalArgumentException.class, () -> SqlIdentifierValidator.validate("tabela de mensagens")); // espaço
        assertThrows(IllegalArgumentException.class, () -> SqlIdentifierValidator.validate(null));
    }
}
