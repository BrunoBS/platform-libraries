package br.com.portalmanager.platform.library.observability.logging.converter;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.ApiErrorResponse;
import br.com.portalmanager.platform.library.messaging.model.ApiValidationDetail;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;
import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizer;
import br.com.portalmanager.platform.library.observability.logging.sanitizer.LogSanitizers;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonErrorMdcConverterTest {

    private final JsonErrorMdcConverter converter = new JsonErrorMdcConverter();

    @AfterEach
    void tearDown() {
        LogSanitizers.configureAdditionalSensitiveFields(Set.of());
    }

    @Test
    void shouldReturnNullWhenStructuredErrorIsMissing() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getMDCPropertyMap()).thenReturn(Map.of("correlationId", "corr-01"));

        assertThat(converter.convert(event)).isEqualTo("null");
    }

    @Test
    void shouldReturnStructuredErrorFromWrappedPlatformConfigurationException() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        PlatformConfigurationException platformException =
                new PlatformConfigurationException(
                        new PlatformErrorDefinition(
                                "PLT-MSG-001",
                                "spring.application.name is required when platform.messaging is enabled",
                                "Configure spring.application.name with the service name.",
                                500
                        )
                );
        RuntimeException wrapped = new RuntimeException("bootstrap failed", platformException);

        when(event.getMDCPropertyMap()).thenReturn(Map.of());
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(wrapped));

        String converted = converter.convert(event);

        assertThat(converted).contains("\"code\":\"PLT-MSG-001\"");
        assertThat(converted).contains(
                "\"message\":\"spring.application.name is required when platform.messaging is enabled\""
        );
        assertThat(converted).contains(
                "\"solution\":\"Configure spring.application.name with the service name.\""
        );
        assertThat(converted).contains("\"details\":[]");
    }

    @Test
    void shouldPreferStructuredMdcErrorOverThrowableError() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        String structuredError = """
                {"code":"GLOBAL-0001","message":"Validation failed","details":[]}
                """.trim();

        PlatformConfigurationException platformException =
                new PlatformConfigurationException(
                        new PlatformErrorDefinition(
                                "PLT-MSG-001",
                                "bootstrap failed",
                                "configure",
                                500
                        )
                );

        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                JsonErrorMdcConverter.ERROR_MDC_KEY,
                structuredError
        ));
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(platformException));

        assertThat(converter.convert(event)).isEqualTo(structuredError);
    }

    @Test
    void shouldReturnStructuredErrorAsRawJson() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        String structuredError = """
                {"code":"GLOBAL-0001","message":"Validation failed","details":[]}
                """.trim();

        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                JsonErrorMdcConverter.ERROR_MDC_KEY,
                structuredError
        ));

        assertThat(converter.convert(event)).isEqualTo(structuredError);
    }

    @org.junit.jupiter.api.Test
    void shouldSanitizeSensitiveDataFromStructuredMdcError() {
        ch.qos.logback.classic.spi.ILoggingEvent event = org.mockito.Mockito.mock(ch.qos.logback.classic.spi.ILoggingEvent.class);
        org.mockito.Mockito.when(event.getMDCPropertyMap()).thenReturn(java.util.Map.of(
                JsonErrorMdcConverter.ERROR_MDC_KEY,
                "{\"code\":\"ERR\",\"message\":\"password=secret\",\"details\":[\"bruno@example.com\"]}"
        ));

        String converted = new JsonErrorMdcConverter().convert(event);

        org.assertj.core.api.Assertions.assertThat(converted)
                .doesNotContain("bruno@example.com")
                .contains("b****@example.com");
    }

    @Test
    void shouldSanitizeEachValidationDetailOnlyOnce() {
        AtomicInteger detailSanitizations = new AtomicInteger();
        LogSanitizer sanitizer = value -> {
            if (value.startsWith("ApiValidationDetail[")) {
                detailSanitizations.incrementAndGet();
            }
            return value;
        };
        LogSanitizers.register(sanitizer);

        try {
            ILoggingEvent event = mock(ILoggingEvent.class);
            PlatformConfigurationException exception = new DetailedConfigurationException();
            when(event.getMDCPropertyMap()).thenReturn(Map.of());
            when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(exception));

            String converted = converter.convert(event);

            assertThat(converted).contains("detail-value");
            assertThat(detailSanitizations).hasValue(1);
        } finally {
            LogSanitizers.unregister(sanitizer);
        }
    }

    private static final class DetailedConfigurationException extends PlatformConfigurationException {
        private final ApiErrorResponse response = new ApiErrorResponse(
                "ERR",
                "message",
                "solution",
                List.of(new ApiValidationDetail("field", "detail-value")),
                Instant.EPOCH,
                null,
                null
        );

        private DetailedConfigurationException() {
            super(new PlatformErrorDefinition("ERR", "message", "solution", 500));
        }

        @Override
        public ApiErrorResponse getErrorResponse() {
            return response;
        }
    }
}
