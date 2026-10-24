package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationContextExecutorServiceTest {

    @Test
    void shouldPropagateTheCurrentUserAndRestoreTheWorkerContext() throws Exception {
        UserSession testUser = AuthorizationSessionBuilder.builder().userName("test-user").build();
        UserSession workerUser = AuthorizationSessionBuilder.builder().userName("worker-user").build();
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        try {
            delegate.submit(() -> UserContext.set(workerUser)).get();
            UserContext.set(testUser);

            ExecutorService executor = AuthorizationContextExecutorService.wrap(delegate);
            try {
                assertThat(executor.submit(() -> UserContext.get().orElseThrow()).get())
                        .isSameAs(testUser);
                assertThat(delegate.submit(() -> UserContext.get().orElseThrow()).get())
                        .isSameAs(workerUser);
            } finally {
                executor.shutdownNow();
            }

            assertThat(UserContext.get()).contains(testUser);
        } finally {
            UserContext.clear();
            delegate.shutdownNow();
        }
    }

    @Test
    void shouldRestoreTheWorkerContextWhenATaskFails() throws Exception {
        UserSession testUser = AuthorizationSessionBuilder.builder().userName("test-user").build();
        UserSession workerUser = AuthorizationSessionBuilder.builder().userName("worker-user").build();
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        try {
            delegate.submit(() -> UserContext.set(workerUser)).get();
            UserContext.set(testUser);

            ExecutorService executor = AuthorizationContextExecutorService.wrap(delegate);
            try {
                assertThatThrownBy(() -> executor.submit(() -> {
                    assertThat(UserContext.get()).contains(testUser);
                    throw new IllegalStateException("expected test failure");
                }).get()).isInstanceOf(ExecutionException.class)
                        .hasCauseInstanceOf(IllegalStateException.class);
                assertThat(delegate.submit(() -> UserContext.get().orElseThrow()).get())
                        .isSameAs(workerUser);
            } finally {
                executor.shutdownNow();
            }
        } finally {
            UserContext.clear();
            delegate.shutdownNow();
        }
    }

    @Test
    void shouldRejectWrappingAnExecutorWithoutAUserContext() {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        try {
            assertThatThrownBy(() -> AuthorizationContextExecutorService.wrap(delegate))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("user context must be established");
        } finally {
            delegate.shutdownNow();
        }
    }
}
