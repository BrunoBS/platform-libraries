package br.com.portalmanager.platform.observability.logging.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonErrorMdcConverterTest {

    private final JsonErrorMdcConverter converter = new JsonErrorMdcConverter();

    @Test
    void shouldReturnNullWhenStructuredErrorIsMissing() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of("correlationId", "corr-01"));

        assertThat(converter.convert(event)).isEqualTo("null");
    }

    @Test
    void shouldReturnStructuredErrorAsRawJson() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        String structuredError = """
                {"code":"GLOBAL-0001","message":"Validation failed","details":[]}
                """.trim();

        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                JsonErrorMdcConverter.ERROR_MDC_KEY,
                structuredError
        ));

        assertThat(converter.convert(event)).isEqualTo(structuredError);
    }
}
