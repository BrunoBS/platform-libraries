package br.com.portalmanager.platform.library.observability.logging.converter;

import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizers;
import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String originalMessage = super.convert(event);
        if (originalMessage == null || originalMessage.isBlank()) {
            return originalMessage;
        }

        return JsonMessageConverter.escapeJson(LogSanitizers.sanitize(originalMessage));
    }
}
