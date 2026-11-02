package br.com.portalmanager.platform.library.observability.logging.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonMdcConverter extends ClassicConverter {

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
                .map(entry -> "\"" + JsonMessageConverter.escapeJson(entry.getKey()) + "\":\"" +
                        JsonMessageConverter.escapeJson(entry.getValue()) + "\"")
                .collect(Collectors.joining(","));

        return fields.isEmpty() ? "{}" : "{" + fields + "}";
    }
}
