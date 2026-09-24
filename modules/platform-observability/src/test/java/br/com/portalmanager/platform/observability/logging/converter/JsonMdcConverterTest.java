package br.com.portalmanager.platform.observability.logging.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonMdcConverterTest {

    private final JsonMdcConverter converter = new JsonMdcConverter();

    @Test
    void shouldReturnEmptyJsonObjectWhenMdcIsEmpty() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of());

        assertThat(converter.convert(event)).isEqualTo("{}");
    }

    @Test
    void shouldSerializeMdcAsJsonObject() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("accountId", "AC-01");
        mdc.put("environmentId", "DEV");
        mdc.put("username", "bbs");
        when(event.getMDCPropertyMap()).thenReturn(mdc);

        assertThat(converter.convert(event))
                .isEqualTo("{\"accountId\":\"AC-01\",\"environmentId\":\"DEV\",\"username\":\"bbs\"}");
    }

    @Test
    void shouldExcludeStructuredErrorFromContext() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                "correlationId", "corr-01",
                JsonErrorMdcConverter.ERROR_MDC_KEY, "{\"code\":\"GLOBAL-0001\"}"
        ));

        assertThat(converter.convert(event))
                .isEqualTo("{\"correlationId\":\"corr-01\"}");
    }

    @Test
    void shouldEscapeSpecialCharacters() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                "uri", "/api/\"workspace\"",
                "userAgent", "line1\nline2\\client"
        ));

        assertThat(converter.convert(event))
                .isEqualTo("{\"uri\":\"/api/\\\"workspace\\\"\",\"userAgent\":\"line1\\nline2\\\\client\"}");
    }
}
