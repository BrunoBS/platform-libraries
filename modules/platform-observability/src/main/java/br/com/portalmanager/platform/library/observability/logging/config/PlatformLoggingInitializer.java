package br.com.portalmanager.platform.library.observability.logging.config;

import br.com.portalmanager.platform.library.observability.logging.converter.JsonErrorMdcConverter;
import br.com.portalmanager.platform.library.observability.logging.converter.JsonMdcConverter;
import br.com.portalmanager.platform.library.observability.logging.converter.JsonMessageConverter;
import br.com.portalmanager.platform.library.observability.logging.converter.JsonThrowableConverter;
import br.com.portalmanager.platform.library.observability.logging.converter.MaskingConverter;
import br.com.portalmanager.platform.library.observability.logging.metadata.BuildVersionResolver;
import br.com.portalmanager.platform.library.observability.logging.metadata.HostResolver;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.NopStatusListener;
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
            ClassPathResource resource = new ClassPathResource("platform-observability-defaults.properties");
            if (resource.exists()) {
                env.getPropertySources().addLast(new ResourcePropertySource("platformLoggingDefaults", resource));
            }
        } catch (IOException ignored) {
        }
    }

    private ConsoleAppender<ILoggingEvent> createJsonConsoleAppender(
            LoggerContext loggerContext,
            ConfigurableEnvironment env
    ) {
        String serviceName = env.getProperty("spring.application.name", "unknown-service");
        String appVersion = BuildVersionResolver.resolve(env);
        String host = HostResolver.resolve(env);
        boolean maskingEnabled = env.getProperty(
                "platform.observability.logging.masking.enabled",
                Boolean.class,
                true
        );

        Map<String, String> customConverters = Binder.get(env)
                .bind(
                        "platform.observability.logging.custom-converters",
                        Bindable.mapOf(String.class, String.class)
                )
                .orElse(new HashMap<>());

        registerPatternConverters(loggerContext, maskingEnabled, customConverters);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(buildJsonPattern(serviceName, appVersion, host, maskingEnabled, customConverters));
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setName(APPENDER_NAME);
        appender.setEncoder(encoder);
        appender.start();

        return appender;
    }

    @SuppressWarnings("unchecked")
    private void registerPatternConverters(
            LoggerContext loggerContext,
            boolean maskingEnabled,
            Map<String, String> customConverters
    ) {
        Map<String, String> converters = new HashMap<>();
        Object registeredConverters = loggerContext.getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (registeredConverters instanceof Map<?, ?> existingConverters) {
            existingConverters.forEach((key, value) -> {
                if (key instanceof String converterName && value instanceof String converterClass) {
                    converters.put(converterName, converterClass);
                }
            });
        }

        converters.put("jsonMessage", JsonMessageConverter.class.getName());
        converters.put("jsonMdc", JsonMdcConverter.class.getName());
        converters.put("jsonError", JsonErrorMdcConverter.class.getName());
        converters.put("jsonThrowable", JsonThrowableConverter.class.getName());
        if (maskingEnabled) {
            converters.put("corporateLgpdMask", MaskingConverter.class.getName());
        }

        customConverters.forEach((wordTag, className) -> {
            validateCustomConverter(wordTag, className);
            converters.put(wordTag, className);
        });

        loggerContext.putObject(CoreConstants.PATTERN_RULE_REGISTRY, converters);
    }

    private void validateCustomConverter(String wordTag, String className) {
        if (wordTag == null || wordTag.isBlank() || className == null || className.isBlank()) {
            throw new IllegalStateException("Custom logging converter tag and class must not be blank");
        }
        try {
            Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Custom logging converter class not found for tag '" + wordTag + "': " + className,
                    exception
            );
        }
    }

    /**
     * Monta a String do template estruturado do JSON aplicando o envelopamento infinito em cascata.
     */
    private String buildJsonPattern(
            String serviceName,
            String appVersion,
            String host,
            boolean maskingEnabled,
            Map<String, String> customConverters
    ) {
        String messageToken = maskingEnabled ? "%corporateLgpdMask" : "%jsonMessage";
        for (String userTag : customConverters.keySet()) {
            messageToken = String.format("%%%s({%s})", userTag, messageToken);
        }

        return String.format(
                "{\"timestamp\":\"%%d{yyyy-MM-dd'T'HH:mm:ss.SSSX,UTC}\",\"level\":\"%%level\",\"thread\":\"%%thread\",\"logger\":\"%%logger\",\"message\":\"%s\",\"service\":\"%s\",\"version\":\"%s\",\"host\":\"%s\",\"context\":%%jsonMdc,\"error\":%%jsonError,\"exception\":%%jsonThrowable}%%n",
                messageToken,
                serviceName,
                appVersion,
                host
        );
    }

    private void registerAppender(LoggerContext loggerContext, ConsoleAppender<ILoggingEvent> appender) {
        ch.qos.logback.classic.Logger rootLogger =
                loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        Appender<ILoggingEvent> previousPlatformAppender = rootLogger.getAppender(APPENDER_NAME);
        if (previousPlatformAppender != null) {
            rootLogger.detachAppender(previousPlatformAppender);
            previousPlatformAppender.stop();
        }
        rootLogger.addAppender(appender);
    }

    private void configureLogLevels(LoggerContext loggerContext, ConfigurableEnvironment env) {
        Map<String, String> defaultLevels = Binder.get(env)
                .bind("platform.observability.logging.defaults", Bindable.mapOf(String.class, String.class))
                .orElse(new HashMap<>());

        defaultLevels.forEach((packageName, levelStr) -> {
            String springStandardOverride = env.getProperty("logging.level." + packageName);
            String finalLevel = (springStandardOverride != null) ? springStandardOverride : levelStr;
            loggerContext.getLogger(packageName).setLevel(Level.toLevel(finalLevel, Level.INFO));
        });

        Map<String, String> customLevels = Binder.get(env)
                .bind("platform.observability.logging.levels", Bindable.mapOf(String.class, String.class))
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
