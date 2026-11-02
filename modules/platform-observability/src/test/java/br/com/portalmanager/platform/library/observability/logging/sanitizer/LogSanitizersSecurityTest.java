package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void shouldMaskDefaultAndConfiguredFieldsOnceIncludingEscapedAndCompositeValues() {
        LogSanitizers.configureAdditionalSensitiveFields(Set.of("PASSWORD", "privateKey"));

        String sanitized = LogSanitizers.sanitize(
                "{\"password\":\"before\\\"after\",\"privateKey\":{\"nested\":\"secret\"},"
                        + "\"token\":[\"first\",{\"value\":\"second\"}],\"visible\":\"safe\"}"
        );

        assertThat(sanitized)
                .isEqualTo("{\"password\":\"***\",\"privateKey\":\"***\","
                        + "\"token\":\"***\",\"visible\":\"safe\"}")
                .doesNotContain("before", "after", "nested", "secret", "first", "second");
    }

    @Test
    void shouldNotTreatSensitiveFieldTextInsideAnotherStringAsAJsonField() {
        String sanitized = LogSanitizers.sanitize(
                "{\"message\":\"literal \\\"password\\\": \\\"keep-visible\\\"\",\"password\":\"secret\"}"
        );

        assertThat(sanitized)
                .isEqualTo("{\"message\":\"literal \\\"password\\\": \\\"keep-visible\\\"\",\"password\":\"***\"}")
                .contains("keep-visible")
                .doesNotContain("secret");
    }

    @Test
    void shouldMaskSensitiveJsonFragmentsInsideFormattedText() {
        String sanitized = LogSanitizers.sanitize(
                "request payload: {\"password\":\"secret\"} was rejected"
        );

        assertThat(sanitized)
                .isEqualTo("request payload: {\"password\":\"***\"} was rejected")
                .doesNotContain("secret");
    }

    @Test
    void shouldRunARegisteredSanitizerOnlyOnceWhenTheSameInstanceIsRegisteredTwice() {
        AtomicInteger invocations = new AtomicInteger();
        LogSanitizer sanitizer = value -> {
            invocations.incrementAndGet();
            return value;
        };

        LogSanitizers.register(sanitizer);
        LogSanitizers.register(sanitizer);
        try {
            assertThat(LogSanitizers.sanitize("ordinary message")).isEqualTo("ordinary message");
            assertThat(invocations).hasValue(1);
        } finally {
            LogSanitizers.unregister(sanitizer);
        }
    }
}
