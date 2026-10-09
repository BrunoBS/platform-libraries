package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationAsyncAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AuthorizationAsyncAutoConfiguration.class)
            .withPropertyValues("platform.authorization.mode=MOCK");

    @AfterEach
    void cleanup() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void disabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean("authorizationExecutor"));
    }

    @Test
    void disabledExplicitly() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("authorizationExecutor"));
    }

    @Test
    void enabledExecutorPropagatesAndRestoresContext() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=true")
                .run(context -> {
                    assertThat(context).hasBean("authorizationContextTaskDecorator");
                    ThreadPoolTaskExecutor executor = context.getBean("authorizationExecutor", ThreadPoolTaskExecutor.class);
                    UserSession session = new UserSession();
                    session.setUserName("alice");
                    UserContext.set(session);
                    MDC.put("correlationId", "request-1");
                    var future = executor.submit(() -> {
                        assertThat(UserContext.get()).containsSame(session);
                        assertThat(MDC.get("correlationId")).isEqualTo("request-1");
                    });
                    future.get(5, TimeUnit.SECONDS);
                    UserContext.clear();
                    MDC.clear();
                    executor.submit(() -> {
                        assertThat(UserContext.get()).isEmpty();
                        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
                    }).get(5, TimeUnit.SECONDS);
                });
    }

    @Test
    void usesSpringTaskExecutionPoolSettings() {
        runner.withPropertyValues(
                        "platform.authorization.context-propagation.enabled=true",
                        "platform.authorization.context-propagation.core-size=3",
                        "platform.authorization.context-propagation.max-size=8",
                        "platform.authorization.context-propagation.queue-capacity=25",
                        "platform.authorization.context-propagation.thread-name-prefix=custom-async-")
                .run(context -> {
                    ThreadPoolTaskExecutor executor =
                            context.getBean("authorizationExecutor", ThreadPoolTaskExecutor.class);
                    assertThat(executor.getCorePoolSize()).isEqualTo(3);
                    assertThat(executor.getMaxPoolSize()).isEqualTo(8);
                    assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(25);
                    assertThat(executor.getThreadNamePrefix()).isEqualTo("custom-async-");
                });
    }

    @Test
    void defaultsAreAvailableWithoutPoolProperties() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=true")
                .run(context -> {
                    ThreadPoolTaskExecutor executor =
                            context.getBean("authorizationExecutor", ThreadPoolTaskExecutor.class);
                    assertThat(executor.getCorePoolSize()).isEqualTo(4);
                    assertThat(executor.getMaxPoolSize()).isEqualTo(16);
                    assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(100);
                    assertThat(context).doesNotHaveBean(org.springframework.scheduling.annotation.AsyncConfigurer.class);
                });
    }

    @Test
    void canSelectDefaultAsyncExecutor() {
        runner.withPropertyValues(
                        "platform.authorization.context-propagation.enabled=true",
                        "platform.authorization.context-propagation.default-executor=true")
                .run(context -> assertThat(context.getBean(
                        org.springframework.scheduling.annotation.AsyncConfigurer.class).getAsyncExecutor())
                        .isSameAs(context.getBean("authorizationExecutor")));
    }

    @Test
    void rejectsInvalidPoolSettings() {
        runner.withPropertyValues(
                        "platform.authorization.context-propagation.enabled=true",
                        "platform.authorization.context-propagation.core-size=20",
                        "platform.authorization.context-propagation.max-size=5")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void preservesApplicationProvidedExecutor() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=true")
                .withUserConfiguration(CustomExecutor.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
                    assertThat(context.getBean("authorizationExecutor"))
                            .isSameAs(context.getBean(CustomExecutor.class).executor);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomExecutor {
        private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        @Bean("authorizationExecutor")
        ThreadPoolTaskExecutor authorizationExecutor() {
            executor.initialize();
            return executor;
        }
    }
}
