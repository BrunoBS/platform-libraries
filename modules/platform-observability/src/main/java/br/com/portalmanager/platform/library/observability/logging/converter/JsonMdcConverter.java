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

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonMdcConverter extends ClassicConverter {

    private static final String REQUEST_BODY_KEY = "requestBody";

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> context = new LinkedHashMap<>();
        Map<String, String> mdc = event.getMDCPropertyMap();

        if (mdc != null) {
            mdc.entrySet().stream()
                    .filter(entry -> !JsonErrorMdcConverter.ERROR_MDC_KEY.equals(entry.getKey()))
                    .forEach(entry -> context.put(entry.getKey(), entry.getValue()));
        }

        resolveRequestBody(event).ifPresent(body -> context.put(REQUEST_BODY_KEY, body));

        if (context.isEmpty()) {
            return "{}";
        }

        return context.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> "\"" + escapeJson(entry.getKey()) + "\":\"" +
                        escapeJson(LogSanitizers.sanitize(entry.getValue())) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private java.util.Optional<String> resolveRequestBody(ILoggingEvent event) {
        if (!event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return java.util.Optional.empty();
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return java.util.Optional.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return java.util.Optional.empty();
        }

        byte[] buffer = wrapper.getContentAsByteArray();
        if (buffer.length == 0) {
            return java.util.Optional.empty();
        }

        String content = new String(buffer, StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ")
                .trim();
        if (content.isBlank()) {
            return java.util.Optional.empty();
        }

        boolean truncated = buffer.length >= PayloadErrorLoggingFilter.MAX_PAYLOAD_SIZE_BYTES;
        return java.util.Optional.of(truncated
                ? content + " ... [PAYLOAD TRUNCATED - EXCEEDED 1MB LIMIT]"
                : content);
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
