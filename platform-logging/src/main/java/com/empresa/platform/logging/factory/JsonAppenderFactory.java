package com.empresa.platform.logging.factory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import com.empresa.platform.logging.constants.LoggingConstants;
import org.springframework.core.env.Environment;

public final class JsonAppenderFactory {

    private JsonAppenderFactory() {}

    public static ConsoleAppender<ILoggingEvent> create(LoggerContext loggerContext, Environment env) {
        String serviceName = env.getProperty("spring.application.name", "unknown-service");
        String appVersion = env.getProperty("info.build.version", "unknown");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(String.format(LoggingConstants.JSON_PATTERN_TEMPLATE, serviceName, appVersion));
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setName(LoggingConstants.APPENDER_NAME);
        appender.setEncoder(encoder);
        appender.start();

        return appender;
    }
}
