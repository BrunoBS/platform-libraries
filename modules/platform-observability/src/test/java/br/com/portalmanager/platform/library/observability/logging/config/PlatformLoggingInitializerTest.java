package br.com.portalmanager.platform.library.observability.logging.config;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        assertThat(appender)
                .as("O appender JSON_CONSOLE deveria estar registrado.")
                .isNotNull();

        assertThat(appender)
                .as("O appender deveria ser uma instância de ConsoleAppender.")
                .isInstanceOf(ConsoleAppender.class);
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

        assertThatCode(() -> initializer.initialize(context))
                .doesNotThrowAnyException();

        assertThat(rootLogger().getAppender("JSON_CONSOLE"))
                .isNotNull();
    }

    @Test
    void shouldInitializeSuccessfullyWithoutApplicationMetadata() {

        assertThatCode(() -> initializer.initialize(context))
                .doesNotThrowAnyException();

        assertThat(rootLogger().getAppender("JSON_CONSOLE"))
                .as("A inicialização deveria funcionar mesmo sem metadados da aplicação.")
                .isNotNull();
    }

    @Test
    void shouldConfigureStructuredJsonContextAndResolveHostFallback() {

        environment.setProperty("HOSTNAME", "null");

        initializer.initialize(context);

        @SuppressWarnings("unchecked")
        ConsoleAppender<ILoggingEvent> appender =
                (ConsoleAppender<ILoggingEvent>) rootLogger().getAppender("JSON_CONSOLE");

        PatternLayoutEncoder encoder = (PatternLayoutEncoder) appender.getEncoder();

        assertThat(encoder.getPattern())
                .as("O contexto deveria usar o conversor JSON do MDC.")
                .contains("\"context\":%jsonMdc");

        assertThat(encoder.getPattern())
                .as("Erros resolvidos deveriam ser emitidos como objeto JSON estruturado.")
                .contains("\"error\":%jsonError");

        assertThat(encoder.getPattern())
                .as("A exceção deveria ser renderizada como campo JSON anulável.")
                .contains("\"exception\":%jsonThrowable");

        assertThat(encoder.getPattern())
                .as("HOSTNAME textual null não deveria ser emitido no log.")
                .doesNotContain("\"host\":\"null\"");
    }

    // ========================================================================
    // LOGBACK STATUS
    // ========================================================================

    @Test
    void shouldRegisterNopStatusListener() {

        initializer.initialize(context);

        assertThat(loggerContext
                        .getStatusManager()
                        .getCopyOfStatusListenerList()
                        .isEmpty())
                .as("O NopStatusListener deveria estar registrado.")
                .isFalse();
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

        assertThat(loggerContext
                        .getLogger("org.springframework")
                        .getLevel())
                .isEqualTo(Level.ERROR);

        assertThat(loggerContext
                        .getLogger("org.hibernate")
                        .getLevel())
                .isEqualTo(Level.ERROR);

        assertThat(loggerContext
                        .getLogger("com.zaxxer.hikari")
                        .getLevel())
                .isEqualTo(Level.ERROR);
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

        assertThat(loggerContext
                        .getLogger("org.springframework")
                        .getLevel())
                .as("A configuração logging.level.* da aplicação deveria sobrescrever o default corporativo.")
                .isEqualTo(Level.DEBUG);

        assertThat(loggerContext
                        .getLogger("org.hibernate")
                        .getLevel())
                .as("O Hibernate deveria continuar utilizando o default corporativo.")
                .isEqualTo(Level.ERROR);
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

        assertThat(loggerContext
                        .getLogger("org.springframework")
                        .getLevel())
                .isEqualTo(Level.DEBUG);

        assertThat(loggerContext
                        .getLogger("org.hibernate")
                        .getLevel())
                .isEqualTo(Level.WARN);
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

        assertThat(loggerContext
                        .getLogger("com.novaequipe.vendas")
                        .getLevel())
                .as("O pacote customizado deveria assumir DEBUG.")
                .isEqualTo(Level.DEBUG);

        assertThat(loggerContext
                        .getLogger("com.novaequipe.vendas.utils")
                        .getLevel())
                .as("O subpacote customizado deveria assumir WARN.")
                .isEqualTo(Level.WARN);
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

        assertThat(loggerContext
                        .getLogger("com.empresa.api")
                        .getLevel())
                .isEqualTo(Level.DEBUG);

        assertThat(loggerContext
                        .getLogger("com.empresa.service")
                        .getLevel())
                .isEqualTo(Level.INFO);

        assertThat(loggerContext
                        .getLogger("com.empresa.repository")
                        .getLevel())
                .isEqualTo(Level.WARN);

        assertThat(loggerContext
                        .getLogger("com.empresa.integration")
                        .getLevel())
                .isEqualTo(Level.ERROR);
    }

    // ========================================================================
    // EMPTY CONFIGURATION
    // ========================================================================

    @Test
    void shouldInitializeSuccessfullyWhenNoCustomLevelsAreConfigured() {

        assertThatCode(() -> initializer.initialize(context))
                .doesNotThrowAnyException();

        Logger logger =
                loggerContext.getLogger("com.brunobs");

        /*
         * O nível explícito pode ser null porque o logger herda
         * o nível do root logger.
         *
         * Por isso verificamos getEffectiveLevel().
         */
        assertThat(logger.getEffectiveLevel())
                .as("O logger deveria possuir um nível efetivo.")
                .isNotNull();

        assertThat(logger.getEffectiveLevel())
                .isEqualTo(Level.INFO);
    }

    @Test
    void shouldInitializeSuccessfullyWhenOnlyDefaultsAreConfigured() {

        environment.setProperty(
                "platform.observability.logging.defaults.com.empresa",
                "WARN"
        );

        assertThatCode(() -> initializer.initialize(context))
                .doesNotThrowAnyException();

        assertThat(loggerContext
                        .getLogger("com.empresa")
                        .getLevel())
                .isEqualTo(Level.WARN);
    }

    @Test
    void shouldInitializeSuccessfullyWhenOnlyCustomLevelsAreConfigured() {

        environment.setProperty(
                "platform.observability.logging.levels.com.empresa",
                "DEBUG"
        );

        assertThatCode(() -> initializer.initialize(context))
                .doesNotThrowAnyException();

        assertThat(loggerContext
                        .getLogger("com.empresa")
                        .getLevel())
                .isEqualTo(Level.DEBUG);
    }

    // ========================================================================
    // REINITIALIZATION
    // ========================================================================

    @Test
    void shouldReplacePreviousJsonAppenderWhenInitializedAgain() {

        initializer.initialize(context);

        Appender<ILoggingEvent> firstAppender =
                rootLogger().getAppender("JSON_CONSOLE");

        assertThat(firstAppender)
                .isNotNull();

        initializer.initialize(context);

        Appender<ILoggingEvent> secondAppender =
                rootLogger().getAppender("JSON_CONSOLE");

        assertThat(secondAppender)
                .isNotNull();

        assertThat(secondAppender)
                .as("Uma nova inicialização deveria recriar o appender.")
                .isNotSameAs(firstAppender);
    }

    @Test
    void shouldPreserveApplicationAppenderWhenPlatformLoggingIsInitialized() {
        ConsoleAppender<ILoggingEvent> applicationAppender = new ConsoleAppender<>();
        applicationAppender.setContext(loggerContext);
        applicationAppender.setName("APPLICATION_APPENDER");
        applicationAppender.start();
        rootLogger().addAppender(applicationAppender);

        initializer.initialize(context);

        assertThat(rootLogger().getAppender("APPLICATION_APPENDER"))
                .isNotNull();
        assertThat(rootLogger().getAppender("JSON_CONSOLE"))
                .isNotNull();
    }

    @Test
    void shouldFailFastWhenCustomConverterClassDoesNotExist() {
        environment.setProperty(
                "platform.observability.logging.custom-converters.audit",
                "com.example.DoesNotExistConverter"
        );

        assertThatThrownBy(() -> initializer.initialize(context))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DoesNotExistConverter");
    }

    // ========================================================================
    // ORDER
    // ========================================================================

    @Test
    void shouldHaveHighestPrecedence() {

        assertThat(initializer.getOrder())
                .as("O inicializador deveria possuir HIGHEST_PRECEDENCE.")
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE);
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