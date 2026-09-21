package br.com.portalmanager.platform.observability.logging.converter;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class JsonThrowableConverter extends ThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return JsonMessageConverter.escapeJson(super.convert(event));
    }
}
