package br.com.portalmanager.platform.library.observability.logging.converter;

import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizers;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonThrowableConverterTest {

    private final JsonThrowableConverter converter = new JsonThrowableConverter();

    @AfterEach
    void tearDown() {
        LogSanitizers.configureAdditionalSensitiveFields(Set.of());
    }

    @Test
    void shouldReturnJsonNullWhenEventHasNoThrowable() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getThrowableProxy()).thenReturn(null);

        assertThat(converter.convert(event)).isEqualTo("null");
    }

    @Test
    void shouldReturnQuotedJsonStringWhenThrowableExists() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        ThrowableProxy throwableProxy =
                new ThrowableProxy(new IllegalStateException("boom"));

        when(event.getThrowableProxy()).thenReturn(throwableProxy);

        String converted = converter.convert(event);

        assertThat(converted).startsWith("\"");
        assertThat(converted).endsWith("\"");
        assertThat(converted).contains("java.lang.IllegalStateException");
        assertThat(converted).contains("boom");
        assertThat(converted).contains("\\n");
    }

    @Test
    void shouldSanitizeSensitiveFieldsInThrowableOutput() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        ThrowableProxy throwableProxy = new ThrowableProxy(
                new IllegalStateException("request failed: {\"password\":\"secret\"}")
        );
        when(event.getThrowableProxy()).thenReturn(throwableProxy);

        String converted = converter.convert(event);

        assertThat(converted)
                .contains("\\\"password\\\":\\\"***\\\"")
                .doesNotContain("secret");
    }
}
