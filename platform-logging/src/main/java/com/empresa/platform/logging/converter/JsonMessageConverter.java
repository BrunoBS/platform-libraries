package com.empresa.platform.logging.converter;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class JsonMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return escapeJson(super.convert(event));
    }

    static String escapeJson(String value) {
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
