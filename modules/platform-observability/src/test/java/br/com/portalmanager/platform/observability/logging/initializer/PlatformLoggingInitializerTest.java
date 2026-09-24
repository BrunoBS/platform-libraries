package br.com.portalmanager.platform.observability.logging.initializer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class PlatformLoggingInitializerTest {

    private PlatformLoggingInitializer initializer;
    private ConfigurableApplicationContext context;
    private MockEnvironment environment;
    private LoggerContext loggerContext;

    @BeforeEach
    void setUp() {

        initializer = new PlatformLoggingInitializer();

        environment = new MockEnvironment();

        GenericApplicationContext applicationContext =
                new GenericApplicationContext();

        applicationContext.setEnvironment(environment);

        context = applicationContext;

        loggerContext =
                (LoggerContext) LoggerFactory.getILoggerFactory();

        resetLogback();
    }

    @AfterEach
    void tearDown() {

        resetLogback();

        if (context != null) {
            context.close();
        }
    }

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @Test
    void shouldInitializeJsonConsoleAppender() {

        initializer.initialize(context);

        Logger rootLogger = rootLogger();

        Appender<ILoggingEvent> appender =
                rootLogger.getAppender("JSON_CONSOLE");

        assertNotNull(
                appender,
                "O appender JSON_CONSOLE deveria estar registrado."
        );

        assertInstanceOf(
                ConsoleAppender.class,
                appender,
                "O appender deveria ser uma instância de ConsoleAppender."
        );
    }

    @Test
    void shouldInitializeSuccessfullyWithApplicationMetadata() {

        environment.setProperty(
                "spring.application.name",
                "api-checkout-vendas"
        );

        environment.setProperty(
                "info.build.version",
                "2.4.1-RELEASE"
        );

        assertDoesNotThrow(
                () -> initializer.initialize(context)
        );

        assertNotNull(
                rootLogger().getAppender("JSON_CONSOLE")
        );
    }

    @Test
    void shouldInitializeSuccessfullyWithoutApplicationMetadata() {

        assertDoesNotThrow(
                () -> initializer.initialize(context)
        );

        assertNotNull(
                rootLogger().getAppender("JSON_CONSOLE"),
                "A inicialização deveria funcionar mesmo sem metadados da aplicação."
        );
    }

    @Test
    void shouldConfigureStructuredJsonContextAndResolveHostFallback() {

        environment.setProperty("HOSTNAME", "null");

        initializer.initialize(context);

        @SuppressWarnings("unchecked")
        ConsoleAppender<ILoggingEvent> appender =
                (ConsoleAppender<ILoggingEvent>) rootLogger().getAppender("JSON_CONSOLE");

        PatternLayoutEncoder encoder = (PatternLayoutEncoder) appender.getEncoder();

        assertTrue(
                encoder.getPattern().contains("\"context\":%jsonMdc"),
                "O contexto deveria usar o conversor JSON do MDC."
        );

        assertFalse(
                encoder.getPattern().contains("\"host\":\"null\""),
                "HOSTNAME textual null não deveria ser emitido no log."
        );
    }

    // ========================================================================
    // LOGBACK STATUS
    // ========================================================================

    @Test
    void shouldRegisterNopStatusListener() {

        initializer.initialize(context);

        assertFalse(
                loggerContext
                        .getStatusManager()
                        .getCopyOfStatusListenerList()
                        .isEmpty(),
                "O NopStatusListener deveria estar registrado."
        );
    }

    // ========================================================================
    // DEFAULT LOG LEVELS
    // ========================================================================

    @Test
    void shouldApplyCorporateDefaultLogLevels() {

        environment.setProperty(
                "platform.observability.logging.defaults.org.springframework",
                "ERROR"
        );

        environment.setProperty(
                "platform.observability.logging.defaults.org.hibernate",
                "ERROR"
        );

        environment.setProperty(
                "platform.observability.logging.defaults.com.zaxxer.hikari",
                "ERROR"
        );

        initializer.initialize(context);

        assertEquals(
                Level.ERROR,
                loggerContext
                        .getLogger("org.springframework")
                        .getLevel()
        );

        assertEquals(
                Level.ERROR,
                loggerContext
                        .getLogger("org.hibernate")
                        .getLevel()
        );

        assertEquals(
                Level.ERROR,
                loggerContext
                        .getLogger("com.zaxxer.hikari")
                        .getLevel()
        );
    }

    // ========================================================================
    // SPRING BOOT OVERRIDE
    // ========================================================================

    @Test
    void shouldGiveApplicationLoggingConfigurationPrecedenceOverCorporateDefaults() {

        environment.setProperty(
                "platform.observability.logging.defaults.org.springframework",
                "ERROR"
        );

        environment.setProperty(
                "platform.observability.logging.defaults.org.hibernate",
                "ERROR"
        );

        environment.setProperty(
                "logging.level.org.springframework",
                "DEBUG"
        );

        initializer.initialize(context);

        assertEquals(
                Level.DEBUG,
                loggerContext
                        .getLogger("org.springframework")
                        .getLevel(),
                "A configuração logging.level.* da aplicação deveria sobrescrever o default corporativo."
        );

        assertEquals(
                Level.ERROR,
                loggerContext
                        .getLogger("org.hibernate")
                        .getLevel(),
                "O Hibernate deveria continuar utilizando o default corporativo."
        );
    }

    @Test
    void shouldSupportDifferentApplicationOverridesForDifferentPackages() {

        environment.setProperty(
                "platform.observability.logging.defaults.org.springframework",
                "ERROR"
        );

        environment.setProperty(
                "platform.observability.logging.defaults.org.hibernate",
                "ERROR"
        );

        environment.setProperty(
                "logging.level.org.springframework",
                "DEBUG"
        );

        environment.setProperty(
                "logging.level.org.hibernate",
                "WARN"
        );

        initializer.initialize(context);

        assertEquals(
                Level.DEBUG,
                loggerContext
                        .getLogger("org.springframework")
                        .getLevel()
        );

        assertEquals(
                Level.WARN,
                loggerContext
                        .getLogger("org.hibernate")
                        .getLevel()
        );
    }

    // ========================================================================
    // CUSTOM LOG LEVELS
    // ========================================================================

    @Test
    void shouldApplyCustomLogLevelsDynamically() {

        environment.setProperty(
                "platform.observability.logging.levels.com.novaequipe.vendas",
                "DEBUG"
        );

        environment.setProperty(
                "platform.observability.logging.levels.com.novaequipe.vendas.utils",
                "WARN"
        );

        initializer.initialize(context);

        assertEquals(
                Level.DEBUG,
                loggerContext
                        .getLogger("com.novaequipe.vendas")
                        .getLevel(),
                "O pacote customizado deveria assumir DEBUG."
        );

        assertEquals(
                Level.WARN,
                loggerContext
                        .getLogger("com.novaequipe.vendas.utils")
                        .getLevel(),
                "O subpacote customizado deveria assumir WARN."
        );
    }

    @Test
    void shouldSupportMultipleCustomPackages() {

        environment.setProperty(
                "platform.observability.logging.levels.com.empresa.api",
                "DEBUG"
        );

        environment.setProperty(
                "platform.observability.logging.levels.com.empresa.service",
                "INFO"
        );

        environment.setProperty(
                "platform.observability.logging.levels.com.empresa.repository",
                "WARN"
        );

        environment.setProperty(
                "platform.observability.logging.levels.com.empresa.integration",
                "ERROR"
        );

        initializer.initialize(context);

        assertEquals(
                Level.DEBUG,
                loggerContext
                        .getLogger("com.empresa.api")
                        .getLevel()
        );

        assertEquals(
                Level.INFO,
                loggerContext
                        .getLogger("com.empresa.service")
                        .getLevel()
        );

        assertEquals(
                Level.WARN,
                loggerContext
                        .getLogger("com.empresa.repository")
                        .getLevel()
        );

        assertEquals(
                Level.ERROR,
                loggerContext
                        .getLogger("com.empresa.integration")
                        .getLevel()
        );
    }

    // ========================================================================
    // EMPTY CONFIGURATION
    // ========================================================================

    @Test
    void shouldInitializeSuccessfullyWhenNoCustomLevelsAreConfigured() {

        assertDoesNotThrow(
                () -> initializer.initialize(context)
        );

        Logger logger =
                loggerContext.getLogger("com.brunobs");

        /*
         * O nível explícito pode ser null porque o logger herda
         * o nível do root logger.
         *
         * Por isso verificamos getEffectiveLevel().
         */
        assertNotNull(
                logger.getEffectiveLevel(),
                "O logger deveria possuir um nível efetivo."
        );

        assertEquals(
                Level.INFO,
                logger.getEffectiveLevel()
        );
    }

    @Test
    void shouldInitializeSuccessfullyWhenOnlyDefaultsAreConfigured() {

        environment.setProperty(
                "platform.observability.logging.defaults.com.empresa",
                "WARN"
        );

        assertDoesNotThrow(
                () -> initializer.initialize(context)
        );

        assertEquals(
                Level.WARN,
                loggerContext
                        .getLogger("com.empresa")
                        .getLevel()
        );
    }

    @Test
    void shouldInitializeSuccessfullyWhenOnlyCustomLevelsAreConfigured() {

        environment.setProperty(
                "platform.observability.logging.levels.com.empresa",
                "DEBUG"
        );

        assertDoesNotThrow(
                () -> initializer.initialize(context)
        );

        assertEquals(
                Level.DEBUG,
                loggerContext
                        .getLogger("com.empresa")
                        .getLevel()
        );
    }

    // ========================================================================
    // REINITIALIZATION
    // ========================================================================

    @Test
    void shouldReplacePreviousJsonAppenderWhenInitializedAgain() {

        initializer.initialize(context);

        Appender<ILoggingEvent> firstAppender =
                rootLogger().getAppender("JSON_CONSOLE");

        assertNotNull(firstAppender);

        initializer.initialize(context);

        Appender<ILoggingEvent> secondAppender =
                rootLogger().getAppender("JSON_CONSOLE");

        assertNotNull(secondAppender);

        assertNotSame(
                firstAppender,
                secondAppender,
                "Uma nova inicialização deveria recriar o appender."
        );
    }

    // ========================================================================
    // ORDER
    // ========================================================================

    @Test
    void shouldHaveHighestPrecedence() {

        assertEquals(
                Ordered.HIGHEST_PRECEDENCE,
                initializer.getOrder(),
                "O inicializador deveria possuir HIGHEST_PRECEDENCE."
        );
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private Logger rootLogger() {

        return loggerContext.getLogger(
                Logger.ROOT_LOGGER_NAME
        );
    }

    private void resetLogback() {

        loggerContext.getStatusManager().clear();
        Logger rootLogger = rootLogger();
        rootLogger.detachAndStopAllAppenders();
        rootLogger.setLevel(Level.INFO);

        /*
         * LoggerContext é global na JVM.
         *
         * Os loggers criados durante um teste permanecem no contexto.
         * Portanto removemos níveis explicitamente definidos para evitar
         * vazamento de configuração entre os testes.
         */
        for (Logger logger : loggerContext.getLoggerList()) {

            if (!Logger.ROOT_LOGGER_NAME.equals(logger.getName())) {
                logger.setLevel(null);
            }
        }
    }
}