package br.com.portalmanager.platform.library.observability.logging.converter;

import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizers;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class JsonThrowableConverter extends ThrowableProxyConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String throwable = super.convert(event);

        if (throwable == null || throwable.isBlank()) {
            return "null";
        }

        return "\"" + JsonMessageConverter.escapeJson(LogSanitizers.sanitize(throwable)) + "\"";
    }
}
