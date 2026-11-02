package br.com.portalmanager.platform.library.observability.logging.converter;

import br.com.portalmanager.platform.library.observability.logging.SensitiveValue;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.MessageFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonMessageConverterTest {

    private final JsonMessageConverter converter = new JsonMessageConverter();

    @BeforeEach
    void setUp() {
        SensitiveValue.setMaskingEnabled(true);
    }

    @Test
    void shouldRenderExplicitSensitiveValueWithTheSameMaskInJsonMessages() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        String message = MessageFormatter.arrayFormat(
                "credential={}",
                new Object[]{SensitiveValue.of("abcdefghij")}
        ).getMessage();
        when(event.getFormattedMessage()).thenReturn(message);

        assertThat(converter.convert(event)).isEqualTo("credential=abc****hij");
    }
}
