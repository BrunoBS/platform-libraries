package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class AuthorizationAutoConfiguredAsyncProxyTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformAuthorizationAutoConfiguration.class,
                    AuthorizationAsyncAutoConfiguration.class))
            .withUserConfiguration(AsyncBeans.class)
            .withPropertyValues("platform.authorization.mode=MOCK");

    @AfterEach void cleanup() { UserContext.clear(); MDC.clear(); }

    @Test void enabledDefaultExecutorPropagatesThroughAsyncProxy() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=true",
                        "platform.authorization.context-propagation.default-executor=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    UserContext.set(session("alice"));
                    MDC.put("correlationId", "alice-id");
                    assertThat(context.getBean(AsyncService.class).execute().get(5, TimeUnit.SECONDS))
                            .isEqualTo("alice:alice-id");
                    UserContext.clear();
                    MDC.clear();
                    assertThat(context.getBean(AsyncService.class).execute().get(5, TimeUnit.SECONDS))
                            .isEqualTo("empty:null");
                });
    }

    @Test void disabledPropagationDoesNotCreateAuthorizationExecutor() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("authorizationExecutor");
                });
    }

    @Test void namedExecutorPropagatesWithLibraryAutoConfiguration() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    UserContext.set(session("named"));
                    MDC.put("correlationId", "named-id");
                    assertThat(context.getBean(AsyncService.class).named().get(5, TimeUnit.SECONDS))
                            .isEqualTo("named:named-id");
                });
    }

    @Test void customExecutorIsNotOverriddenByLibrary() {
        runner.withPropertyValues("platform.authorization.context-propagation.enabled=true")
                .withUserConfiguration(CustomExecutor.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("authorizationExecutor"))
                            .isSameAs(context.getBean(CustomExecutor.class).executor);
                });
    }

    private static UserSession session(String name) {
        var session = new UserSession();
        session.setUserName(name);
        return session;
    }

    public static class AsyncService {
        @Async
        public CompletableFuture<String> execute() { return CompletableFuture.completedFuture(current()); }

        @Async("authorizationExecutor")
        public CompletableFuture<String> named() { return CompletableFuture.completedFuture(current()); }

        private String current() {
            return UserContext.get().map(UserSession::getUserName).orElse("empty") + ":" + MDC.get("correlationId");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class AsyncBeans {
        @Bean AsyncService asyncService() { return new AsyncService(); }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomExecutor {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        @Bean("authorizationExecutor")
        ThreadPoolTaskExecutor authorizationExecutor() { return executor; }
    }
}
