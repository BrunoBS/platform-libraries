package br.com.portalmanager.platform.observability.logging.factory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import br.com.portalmanager.platform.observability.logging.constants.LoggingConstants;
import br.com.portalmanager.platform.observability.logging.converter.JsonErrorMdcConverter;
import br.com.portalmanager.platform.observability.logging.converter.JsonMdcConverter;
import br.com.portalmanager.platform.observability.logging.converter.JsonThrowableConverter;
import br.com.portalmanager.platform.observability.logging.metadata.BuildVersionResolver;
import br.com.portalmanager.platform.observability.logging.metadata.HostResolver;
import org.springframework.core.env.Environment;

public final class JsonAppenderFactory {

    private JsonAppenderFactory() {}

    public static ConsoleAppender<ILoggingEvent> create(LoggerContext loggerContext, Environment env) {
        String serviceName = env.getProperty("spring.application.name", "unknown-service");
        String appVersion = BuildVersionResolver.resolve(env);
        String host = HostResolver.resolve(env);

        ch.qos.logback.classic.PatternLayout.defaultConverterMap.put(
                "jsonMdc",
                JsonMdcConverter.class.getName()
        );
        ch.qos.logback.classic.PatternLayout.defaultConverterMap.put(
                "jsonError",
                JsonErrorMdcConverter.class.getName()
        );
        ch.qos.logback.classic.PatternLayout.defaultConverterMap.put(
                "jsonThrowable",
                JsonThrowableConverter.class.getName()
        );

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(String.format(LoggingConstants.JSON_PATTERN_TEMPLATE, serviceName, appVersion, host));
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setName(LoggingConstants.APPENDER_NAME);
        appender.setEncoder(encoder);
        appender.start();

        return appender;
    }

}
