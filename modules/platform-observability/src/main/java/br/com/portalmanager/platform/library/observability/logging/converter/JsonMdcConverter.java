package br.com.portalmanager.platform.library.observability.logging.converter;

import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizers;
import br.com.portalmanager.platform.library.observability.logging.web.PayloadErrorLoggingFilter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonMdcConverter extends ClassicConverter {

    private static final String REQUEST_BODY_KEY = "requestBody";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> context = new LinkedHashMap<>();
        Map<String, String> mdc = event.getMDCPropertyMap();

        if (mdc != null) {
            mdc.entrySet().stream()
                    .filter(entry -> !JsonErrorMdcConverter.ERROR_MDC_KEY.equals(entry.getKey()))
                    .forEach(entry -> context.put(entry.getKey(), entry.getValue()));
        }

        String fields = context.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> "\"" + escapeJson(entry.getKey()) + "\":\"" +
                        escapeJson(LogSanitizers.sanitize(entry.getValue())) + "\"")
                .collect(Collectors.joining(","));

        String bodyField = resolveRequestBody(event)
                .map(this::requestBodyField)
                .orElse("");

        if (fields.isEmpty() && bodyField.isEmpty()) {
            return "{}";
        }

        String separator = !fields.isEmpty() && !bodyField.isEmpty() ? "," : "";
        return "{" + fields + separator + bodyField + "}";
    }

    private Optional<String> resolveRequestBody(ILoggingEvent event) {
        if (event.getLevel() == null || !event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return Optional.empty();
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return Optional.empty();
        }

        byte[] buffer = wrapper.getContentAsByteArray();
        if (buffer.length == 0) {
            return Optional.empty();
        }

        String content = new String(buffer, StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ")
                .trim();
        if (content.isBlank()) {
            return Optional.empty();
        }

        boolean truncated = buffer.length >= PayloadErrorLoggingFilter.MAX_PAYLOAD_SIZE_BYTES;
        return Optional.of(truncated
                ? content + " ... [PAYLOAD TRUNCATED - EXCEEDED 1MB LIMIT]"
                : content);
    }

    private String requestBodyField(String body) {
        String sanitized = LogSanitizers.sanitize(body);
        try {
            JsonNode json = OBJECT_MAPPER.readTree(sanitized);
            return "\"" + REQUEST_BODY_KEY + "\":" + OBJECT_MAPPER.writeValueAsString(json);
        } catch (Exception ignored) {
            return "\"" + REQUEST_BODY_KEY + "\":\"" + escapeJson(sanitized) + "\"";
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
