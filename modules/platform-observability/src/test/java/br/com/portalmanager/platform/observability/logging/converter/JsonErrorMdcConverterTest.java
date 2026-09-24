package br.com.portalmanager.platform.observability.logging.converter;

import br.com.portalmanager.platform.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.messaging.model.PlatformErrorDefinition;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonErrorMdcConverterTest {

    private final JsonErrorMdcConverter converter = new JsonErrorMdcConverter();

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
}
