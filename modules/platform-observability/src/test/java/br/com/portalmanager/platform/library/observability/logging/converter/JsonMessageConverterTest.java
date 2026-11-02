package br.com.portalmanager.platform.library.observability.logging.converter;

import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizers;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonMessageConverterTest {

    private final JsonMessageConverter converter = new JsonMessageConverter();

    @AfterEach
    void tearDown() {
        LogSanitizers.configureAdditionalSensitiveFields(Set.of());
    }

    @Test
    void shouldEscapeMessageWithoutMaskingWhenMaskingConverterIsDisabled() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(
                "{\"password\":\"secret\",\"privateKey\":\"private\",\"contact\":\"person@example.com\"}"
        );
        LogSanitizers.configureAdditionalSensitiveFields(Set.of("privateKey"));

        String converted = converter.convert(event);

        assertThat(converted)
                .isEqualTo("{\\\"password\\\":\\\"secret\\\",\\\"privateKey\\\":\\\"private\\\","
                        + "\\\"contact\\\":\\\"person@example.com\\\"}")
                .contains("secret", "private", "person@example.com")
                .doesNotContain("***");
    }
}
