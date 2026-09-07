package com.empresa.platform.logging.initializer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.NopStatusListener;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventFormattedTimestampJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggerNameJsonProvider;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class PlatformLoggingInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

        loggerContext.getStatusManager().clear();
        NopStatusListener nopStatusListener = new NopStatusListener();
        loggerContext.getStatusManager().add(nopStatusListener);

        rootLogger.detachAndStopAllAppenders();

        ConfigurableEnvironment env = applicationContext.getEnvironment();

        // Carrega o arquivo interno de propriedades padrão de forma transparente no Environment do Spring
        loadInternalDefaults(env);

        String serviceName = env.getProperty("spring.application.name", "unknown-service");
        String appVersion = env.getProperty("info.build.version", "unknown");

        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        encoder.setContext(loggerContext);

        LoggingEventJsonProviders providers = new LoggingEventJsonProviders();
        providers.setContext(loggerContext);

        providers.addTimestamp(new LoggingEventFormattedTimestampJsonProvider() {{ setFieldName("timestamp"); }});
        providers.addLogLevel(new LogLevelJsonProvider() {{ setFieldName("level"); }});
        providers.addThreadName(new ThreadNameJsonProvider() {{ setFieldName("thread"); }});
        providers.addLoggerName(new LoggerNameJsonProvider() {{ setFieldName("logger"); }});
        providers.addMessage(new MessageJsonProvider());

        // AJUSTE: Configuração nativa e segura para o padrão de metadados fixos do ecossistema Cloud
        LoggingEventPatternJsonProvider patternProvider = new LoggingEventPatternJsonProvider();
        patternProvider.setContext(loggerContext);

        // Usamos as propriedades do Spring injetadas estaticamente na String e o marcador nativo %property do Logback para o HOSTNAME
        String patternJson = String.format(
                "{\"service\":\"%s\",\"version\":\"%s\",\"host\":\"%%property{HOSTNAME:-unknown-host}\"}",
                serviceName, appVersion
        );

        patternProvider.setPattern(patternJson);
        providers.addPattern(patternProvider);

        providers.addStackTrace(new StackTraceJsonProvider());
        providers.addMdc(new MdcJsonProvider() {{ setFieldName("context"); }});

        encoder.setProviders(providers);
        encoder.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("JSON_CONSOLE");
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        rootLogger.addAppender(consoleAppender);

        // CONFIGURAÇÃO 100% DINÂMICA E SEM HARDCODE DOS LOGS
        configureLogLevels(loggerContext, env);
    }

    private void loadInternalDefaults(ConfigurableEnvironment env) {
        try {
            ClassPathResource resource = new ClassPathResource("platform-logging-defaults.properties");
            if (resource.exists()) {
                env.getPropertySources().addLast(new ResourcePropertySource("platformLoggingDefaults", resource));
            }
        } catch (IOException ignored) {
            // Ignora silenciosamente se o arquivo interno de fallback falhar
        }
    }

    private void configureLogLevels(LoggerContext loggerContext, ConfigurableEnvironment env) {
        // 1. Extrai o mapa de padrões (Carregado do nosso properties interno)
        Map<String, String> defaultLevels = Binder.get(env)
                .bind("platform.logging.defaults", Bindable.mapOf(String.class, String.class))
                .orElse(new HashMap<>());

        // Aplica os níveis padrões da empresa no Logback de forma iterativa (Zero Hardcode)
        defaultLevels.forEach((packageName, levelStr) -> {
            // Se o desenvolvedor colocou o padrão clássico do Spring no yml (ex: logging.level.org.springframework=DEBUG), ele ganha prioridade
            String springStandardOverride = env.getProperty("logging.level." + packageName);
            String finalLevel = (springStandardOverride != null) ? springStandardOverride : levelStr;

            loggerContext.getLogger(packageName).setLevel(Level.toLevel(finalLevel, Level.INFO));
        });

        // 2. Extrai e aplica o mapa de customizações que o desenvolvedor colocou sob 'platform.logging.levels'
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
