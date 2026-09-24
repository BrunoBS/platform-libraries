package br.com.portalmanager.platform.observability.logging.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;

public class JsonErrorMdcConverter extends ClassicConverter {

    public static final String ERROR_MDC_KEY = "platform.error";

    @Override
    public String convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();

        if (mdc == null || mdc.isEmpty()) {
            return "null";
        }

        String error = mdc.get(ERROR_MDC_KEY);
        if (error == null || error.isBlank()) {
            return "null";
        }

        return error;
    }
}
