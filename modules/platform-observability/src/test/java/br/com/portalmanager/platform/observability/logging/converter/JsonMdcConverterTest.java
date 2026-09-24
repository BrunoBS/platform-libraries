package br.com.portalmanager.platform.observability.logging.converter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonMdcConverterTest {

    private final JsonMdcConverter converter = new JsonMdcConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnEmptyJsonObjectWhenMdcIsEmpty() throws Exception {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of());

        String result = converter.convert(event);

        assertThat(result).isEqualTo("{}");
        assertThat(objectMapper.readTree(result).isObject()).isTrue();
    }

    @Test
    void shouldSerializeMdcAsJsonObject() throws Exception {
        ILoggingEvent event = mock(ILoggingEvent.class);
        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("accountId", "AC-01");
        mdc.put("environmentId", "DEV");
        mdc.put("username", "bbs");
        when(event.getMDCPropertyMap()).thenReturn(mdc);

        String result = converter.convert(event);
        JsonNode json = objectMapper.readTree(result);

        assertThat(json.get("accountId").asText()).isEqualTo("AC-01");
        assertThat(json.get("environmentId").asText()).isEqualTo("DEV");
        assertThat(json.get("username").asText()).isEqualTo("bbs");
    }

    @Test
    void shouldEscapeSpecialCharactersAndKeepValidJson() throws Exception {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                "uri", "/api/\"workspace\"",
                "userAgent", "line1\nline2\\client"
        ));

        String result = converter.convert(event);
        JsonNode json = objectMapper.readTree(result);

        assertThat(json.get("uri").asText()).isEqualTo("/api/\"workspace\"");
        assertThat(json.get("userAgent").asText()).isEqualTo("line1\nline2\\client");
    }
}
