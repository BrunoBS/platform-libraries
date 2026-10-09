package br.com.portalmanager.platform.library.authorization.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationContextPropagationTest {
    @AfterEach
    void cleanup() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void propagatesAndCleansUpOnVirtualThread() throws Exception {
        UserSession session = session("alice");
        UserContext.set(session);
        MDC.put("correlationId", "request-1");
        Runnable task = AuthorizationContextPropagation.wrap(() -> {
            assertThat(UserContext.get()).containsSame(session);
            assertThat(MDC.get("correlationId")).isEqualTo("request-1");
        });
        UserContext.clear();
        MDC.clear();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(task).get();
        }
        assertThat(UserContext.get()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void restoresWorkerContextAfterTaskAndException() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            UserSession request = session("request");
            UserContext.set(request);
            MDC.put("correlationId", "request-id");
            Runnable success = AuthorizationContextPropagation.wrap(() -> {
                assertThat(UserContext.get()).containsSame(request);
                assertThat(MDC.get("correlationId")).isEqualTo("request-id");
            });
            Runnable failure = AuthorizationContextPropagation.wrap(() -> {
                throw new IllegalStateException("boom");
            });
            UserContext.clear();
            MDC.clear();
            executor.submit(success).get();
            assertThatThrownBy(() -> executor.submit(failure).get())
                    .hasCauseInstanceOf(IllegalStateException.class);
            executor.submit(() -> {
                assertThat(UserContext.get()).isEmpty();
                assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
            }).get();
        }
    }

    @Test
    void callablePropagatesSnapshotAndRestoresExistingWorkerValues() throws Exception {
        UserSession request = session("request");
        UserContext.set(request);
        MDC.put("correlationId", "request-id");
        var wrapped = AuthorizationContextPropagation.wrap(() -> {
            assertThat(UserContext.get()).containsSame(request);
            assertThat(MDC.get("correlationId")).isEqualTo("request-id");
            return "ok";
        });
        UserSession previous = session("worker");
        UserContext.set(previous);
        MDC.put("correlationId", "worker-id");
        assertThat(wrapped.call()).isEqualTo("ok");
        assertThat(UserContext.get()).containsSame(previous);
        assertThat(MDC.get("correlationId")).isEqualTo("worker-id");
    }

    @Test
    void decoratorCapturesAtSubmissionAndDoesNotInheritAfterCompletion() {
        UserSession session = session("request");
        UserContext.set(session);
        Runnable decorated = AuthorizationContextPropagation.taskDecorator().decorate(
                () -> assertThat(UserContext.get()).containsSame(session));
        UserContext.clear();
        decorated.run();
        assertThat(UserContext.get()).isEmpty();
    }

    private static UserSession session(String name) {
        UserSession session = new UserSession();
        session.setUserName(name);
        return session;
    }
}
