package br.com.portalmanager.platform.library.authorization.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class AuthorizationContextConcurrencyIntegrationTest {
    @AfterEach void cleanup() { UserContext.clear(); MDC.clear(); }

    @Test void separateRequestsNeverLeakAcrossReusedWorker() throws Exception {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(2);
        executor.setTaskDecorator(AuthorizationContextPropagation.taskDecorator());
        executor.initialize();
        try {
            UserSession first = session("first");
            UserContext.set(first);
            MDC.put("correlationId", "first-id");
            executor.submit(() -> {
                assertThat(UserContext.get()).containsSame(first);
                assertThat(MDC.get("correlationId")).isEqualTo("first-id");
            }).get(5, TimeUnit.SECONDS);

            UserSession second = session("second");
            UserContext.set(second);
            MDC.put("correlationId", "second-id");
            executor.submit(() -> {
                assertThat(UserContext.get()).containsSame(second);
                assertThat(MDC.get("correlationId")).isEqualTo("second-id");
            }).get(5, TimeUnit.SECONDS);

            UserContext.clear();
            MDC.clear();
            executor.submit(() -> {
                assertThat(UserContext.get()).isEmpty();
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            }).get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }
    }

    @Test void workerExceptionDoesNotLeakToNextTask() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            UserContext.set(session("request"));
            MDC.put("correlationId", "request-id");
            var failed = AuthorizationContextPropagation.wrap((Runnable) () -> {
                assertThat(UserContext.get()).isPresent();
                throw new IllegalStateException("expected");
            });
            UserContext.clear();
            MDC.clear();
            assertThatThrownBy(() -> executor.submit(failed).get())
                    .hasCauseInstanceOf(IllegalStateException.class);
            executor.submit(() -> {
                assertThat(UserContext.get()).isEmpty();
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            }).get();
        }
    }

    @Test void virtualThreadPropagatesOnlyWhenExplicitlyWrapped() throws Exception {
        UserSession request = session("virtual");
        UserContext.set(request);
        MDC.put("correlationId", "virtual-id");
        var wrapped = AuthorizationContextPropagation.wrap((Runnable) () -> {
            assertThat(UserContext.get()).containsSame(request);
            assertThat(MDC.get("correlationId")).isEqualTo("virtual-id");
        });
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(wrapped).get();
            executor.submit(() -> {
                assertThat(UserContext.get()).isEmpty();
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            }).get();
        }
        assertThat(UserContext.get()).containsSame(request);
        assertThat(MDC.get("correlationId")).isEqualTo("virtual-id");
    }

    private static UserSession session(String name) {
        UserSession session = new UserSession();
        session.setUserName(name);
        return session;
    }
}
