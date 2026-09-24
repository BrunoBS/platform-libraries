package br.com.portalmanager.platform.observability.logging.converter;

import br.com.portalmanager.platform.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.messaging.model.ApiErrorResponse;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonErrorMdcConverter extends ClassicConverter {

    public static final String ERROR_MDC_KEY = "platform.error";

    @Override
    public String convert(ILoggingEvent event) {
        String mdcError = resolveMdcError(event);
        if (mdcError != null) {
            return mdcError;
        }

        ApiErrorResponse throwableError = resolveThrowableError(event);
        if (throwableError != null) {
            return toJson(throwableError);
        }

        return "null";
    }

    private String resolveMdcError(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return null;
        }

        String error = mdc.get(ERROR_MDC_KEY);
        return error == null || error.isBlank() ? null : error;
    }

    private ApiErrorResponse resolveThrowableError(ILoggingEvent event) {
        if (!(event.getThrowableProxy() instanceof ThrowableProxy throwableProxy)) {
            return null;
        }

        Throwable current = throwableProxy.getThrowable();
        while (current != null) {
            if (current instanceof PlatformConfigurationException configurationException) {
                return configurationException.getErrorResponse();
            }
            current = current.getCause();
        }

        return null;
    }

    private String toJson(ApiErrorResponse response) {
        return "{" +
                "\"code\":" + jsonString(response.code()) + "," +
                "\"message\":" + jsonString(response.message()) + "," +
                "\"solution\":" + jsonString(response.solution()) + "," +
                "\"details\":" + toJsonDetails(response.details()) + "," +
                "\"timestamp\":" + jsonString(
                        response.timestamp() == null ? null : response.timestamp().toString()
                ) + "," +
                "\"path\":" + jsonString(response.path()) + "," +
                "\"correlationId\":" + jsonString(response.correlationId()) +
                "}";
    }

    private String toJsonDetails(List<?> details) {
        if (details == null || details.isEmpty()) {
            return "[]";
        }
        return details.stream()
                .map(String::valueOf)
                .map(this::jsonString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String jsonString(String value) {
        return value == null
                ? "null"
                : "\"" + JsonMessageConverter.escapeJson(value) + "\"";
    }
}
