package com.empresa.platform.logging.initializer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.NopStatusListener;
import com.empresa.platform.logging.converter.JsonMessageConverter;
import com.empresa.platform.logging.converter.JsonThrowableConverter;
import com.empresa.platform.logging.converter.MaskingConverter;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlatformLoggingInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private static final String APPENDER_NAME = "JSON_CONSOLE";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        configureStatusListener(loggerContext);
        loadInternalDefaults(env);

        ConsoleAppender<ILoggingEvent> jsonAppender = createJsonConsoleAppender(loggerContext, env);
        registerAppender(loggerContext, jsonAppender);

        configureLogLevels(loggerContext, env);
    }

    private void configureStatusListener(LoggerContext loggerContext) {
        loggerContext.getStatusManager().clear();
        loggerContext.getStatusManager().add(new NopStatusListener());
    }

    private void loadInternalDefaults(ConfigurableEnvironment env) {
        try {
            ClassPathResource resource = new ClassPathResource("platform-logging-defaults.properties");
            if (resource.exists()) {
                env.getPropertySources().addLast(new ResourcePropertySource("platformLoggingDefaults", resource));
            }
        } catch (IOException ignored) {
        }
    }

    private ConsoleAppender<ILoggingEvent> createJsonConsoleAppender(LoggerContext loggerContext, ConfigurableEnvironment env) {
        String serviceName = env.getProperty("spring.application.name", "unknown-service");
        String appVersion = env.getProperty("info.build.version", "unknown");
        boolean maskingEnabled = env.getProperty("platform.logging.masking.enabled", Boolean.class, true);

        // Captura o mapa de conversores customizados do usuário informados no YAML
        Map<String, String> customConverters = Binder.get(env)
                .bind("platform.logging.custom-converters", Bindable.mapOf(String.class, String.class))
                .orElse(new HashMap<>());

        PatternLayoutEncoder encoder = new PatternLayoutEncoder() {
            @Override
            public void start() {
                ch.qos.logback.classic.PatternLayout.defaultConverterMap.put("jsonMessage", JsonMessageConverter.class.getName());
                ch.qos.logback.classic.PatternLayout.defaultConverterMap.put("jsonThrowable", JsonThrowableConverter.class.getName());
                if (maskingEnabled) {
                    ch.qos.logback.classic.PatternLayout.defaultConverterMap.put("corporateLgpdMask", MaskingConverter.class.getName());
                }

                customConverters.forEach((wordTag, className) -> {
                    try {
                        Class.forName(className);
                        ch.qos.logback.classic.PatternLayout.defaultConverterMap.put(wordTag, className);
                    } catch (ClassNotFoundException ignored) {
                    }
                });

                super.start();
            }
        };

        encoder.setContext(loggerContext);
        encoder.setPattern(buildJsonPattern(serviceName, appVersion, maskingEnabled, customConverters));
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setName(APPENDER_NAME);
        appender.setEncoder(encoder);
        appender.start();

        return appender;
    }

    /**
     * Monta a String do template estruturado do JSON aplicando o envelopamento infinito em cascata.
     */
    private String buildJsonPattern(String serviceName, String appVersion, boolean maskingEnabled, Map<String, String> customConverters) {
        String messageToken = maskingEnabled ? "%corporateLgpdMask" : "%jsonMessage";
        for (String userTag : customConverters.keySet()) {
            messageToken = String.format("%%%s({%s})", userTag, messageToken);
        }

        return String.format(
                "{\"timestamp\":\"%%d{yyyy-MM-dd'T'HH:mm:ss.SSSX,UTC}\",\"level\":\"%%level\",\"thread\":\"%%thread\",\"logger\":\"%%logger\",\"message\":\"%s\",\"service\":\"%s\",\"version\":\"%s\",\"host\":\"%%property{HOSTNAME:-unknown-host}\",\"context\":%%mdc,\"exception\":\"%%jsonThrowable\"}%%n",
                messageToken, serviceName, appVersion
        );
    }

    private void registerAppender(LoggerContext loggerContext, ConsoleAppender<ILoggingEvent> appender) {
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        rootLogger.addAppender(appender);
    }

    private void configureLogLevels(LoggerContext loggerContext, ConfigurableEnvironment env) {
        Map<String, String> defaultLevels = Binder.get(env)
                .bind("platform.logging.defaults", Bindable.mapOf(String.class, String.class))
                .orElse(new HashMap<>());

        defaultLevels.forEach((packageName, levelStr) -> {
            String springStandardOverride = env.getProperty("logging.level." + packageName);
            String finalLevel = (springStandardOverride != null) ? springStandardOverride : levelStr;
            loggerContext.getLogger(packageName).setLevel(Level.toLevel(finalLevel, Level.INFO));
        });

        Map<String, String> customLevels = Binder.get(env)
                .bind("platform.logging.levels", Bindable.mapOf(String.class, String.class))
                .orElse(new HashMap<>());

        customLevels.forEach((packageName, levelStr) ->
                loggerContext.getLogger(packageName).setLevel(Level.toLevel(levelStr, Level.INFO))
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
