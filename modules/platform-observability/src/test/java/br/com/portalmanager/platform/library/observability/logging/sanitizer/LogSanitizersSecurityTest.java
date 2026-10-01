package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizersSecurityTest {

    @AfterEach
    void tearDown() {
        LogSanitizers.configureAdditionalSensitiveFields(Set.of());
    }

    @Test
    void shouldMaskConfiguredSensitiveJsonFields() {
        LogSanitizers.configureAdditionalSensitiveFields(Set.of("privateKey", "credential"));

        String sanitized = LogSanitizers.sanitize(
                "{\"privateKey\":\"super-secret\",\"credential\":\"credential-value\",\"name\":\"workspace\"}"
        );

        assertThat(sanitized)
                .isEqualTo("{\"privateKey\":\"***\",\"credential\":\"***\",\"name\":\"workspace\"}")
                .doesNotContain("super-secret", "credential-value");
    }

    @Test
    void shouldKeepDefaultSensitiveFieldsProtected() {
        String sanitized = LogSanitizers.sanitize(
                "{\"password\":\"secret\",\"authorization\":\"Bearer abc\",\"token\":\"xyz\"}"
        );

        assertThat(sanitized)
                .doesNotContain("secret", "Bearer abc", "xyz")
                .contains("\"password\":\"***\"", "\"authorization\":\"***\"", "\"token\":\"***\"");
    }
}
