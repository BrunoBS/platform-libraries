package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.internal.ListenerSupport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RedisDiagnosticsTest {
    @Test
    void defaultDiagnosticsNeverLogPayloadSecretOrOriginalCause() {
        Logger logger = (Logger) LoggerFactory.getLogger(PlatformRedisAutoConfiguration.class);
        var output = new ListAppender<ILoggingEvent>();
        output.start();
        logger.addAppender(output);
        try {
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    DataRedisAutoConfiguration.class,
                                    DataRedisReactiveAutoConfiguration.class,
                                    PlatformRedisAutoConfiguration.class))
                    .withPropertyValues("spring.application.name=diagnostics")
                    .run(
                            ctx -> {
                                ctx.getBean(ListenerSupport.class)
                                        .failed(
                                                "pubsub",
                                                new RuntimeException("SECRET_PAYLOAD_PASSWORD"));
                                assertThat(output.list).hasSize(1);
                                assertThat(output.list.getFirst().getFormattedMessage())
                                        .contains("redis.listener")
                                        .doesNotContain("SECRET_PAYLOAD_PASSWORD");
                                assertThat(output.list.getFirst().getThrowableProxy()).isNull();
                            });
        } finally {
            logger.detachAppender(output);
            output.stop();
        }
    }
}
