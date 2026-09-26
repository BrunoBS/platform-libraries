package br.com.portalmanager.platform.library.observability.logging.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonMdcConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();

        if (mdc == null || mdc.isEmpty()) {
            return "{}";
        }

        return mdc.entrySet().stream()
                .filter(entry -> !JsonErrorMdcConverter.ERROR_MDC_KEY.equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> "\"" + escapeJson(entry.getKey()) + "\":\"" + escapeJson(entry.getValue()) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
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
