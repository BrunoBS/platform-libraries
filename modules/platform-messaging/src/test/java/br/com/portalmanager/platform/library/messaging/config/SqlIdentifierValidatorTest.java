package br.com.portalmanager.platform.library.messaging.config;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
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
        assertThrows(PlatformConfigurationException.class, () -> SqlIdentifierValidator.validate("vw_api_message; DROP TABLE x;"));
        assertThrows(PlatformConfigurationException.class, () -> SqlIdentifierValidator.validate("vw-api-message")); // hífen
        assertThrows(PlatformConfigurationException.class, () -> SqlIdentifierValidator.validate("tabela de mensagens")); // espaço
        assertThrows(PlatformConfigurationException.class, () -> SqlIdentifierValidator.validate(null));
    }
}
