package com.empresa.platform.logging.converter;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class JsonThrowableConverter extends ThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return escapeJson(super.convert(event));
    }

    private String escapeJson(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace(""", "\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
