package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContextPropagation;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class AuthorizationSpringAsyncIntegrationTest {
    @AfterEach void cleanup() { UserContext.clear(); MDC.clear(); }

    @Test void namedExecutorPropagatesThroughRealAsyncProxy() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(AsyncConfig.class)) {
            UserSession session = session("named");
            UserContext.set(session);
            MDC.put("correlationId", "named-id");
            var result = context.getBean(AsyncService.class).named();
            assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("named:named-id");
            assertThat(UserContext.get()).containsSame(session);
        }
    }

    @Test void defaultExecutorPropagatesThroughRealAsyncProxy() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(AsyncConfig.class)) {
            UserContext.set(session("default"));
            MDC.put("correlationId", "default-id");
            assertThat(context.getBean(AsyncService.class).defaultAsync().get(5, TimeUnit.SECONDS))
                    .isEqualTo("default:default-id");
        }
    }

    @Test void exceptionInAsyncTaskDoesNotContaminateNextTask() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(AsyncConfig.class)) {
            UserContext.set(session("error"));
            MDC.put("correlationId", "error-id");
            assertThatThrownBy(() -> context.getBean(AsyncService.class).failure().get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(IllegalStateException.class);
            UserContext.clear();
            MDC.clear();
            assertThat(context.getBean(AsyncService.class).defaultAsync().get(5, TimeUnit.SECONDS))
                    .isEqualTo("empty:null");
        }
    }

    private static UserSession session(String name) {
        var session = new UserSession();
        session.setUserName(name);
        return session;
    }

    public static class AsyncService {
        @Async("authorizationExecutor")
        public CompletableFuture<String> named() { return CompletableFuture.completedFuture(current()); }

        @Async
        public CompletableFuture<String> defaultAsync() { return CompletableFuture.completedFuture(current()); }

        @Async
        public CompletableFuture<String> failure() { throw new IllegalStateException("expected"); }

        private String current() {
            return UserContext.get().map(UserSession::getUserName).orElse("empty")
                    + ":" + MDC.get("correlationId");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAsync
    static class AsyncConfig {
        @Bean("authorizationExecutor")
        ThreadPoolTaskExecutor executor() {
            var executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(5);
            executor.setTaskDecorator(AuthorizationContextPropagation.taskDecorator());
            return executor;
        }
        @Bean AsyncService asyncService() { return new AsyncService(); }

        @Bean
        org.springframework.scheduling.annotation.AsyncConfigurer asyncConfigurer(
                @org.springframework.beans.factory.annotation.Qualifier("authorizationExecutor")
                ThreadPoolTaskExecutor executor) {
            return new org.springframework.scheduling.annotation.AsyncConfigurer() {
                @Override public java.util.concurrent.Executor getAsyncExecutor() {
                    return executor;
                }
            };
        }
    }
}
