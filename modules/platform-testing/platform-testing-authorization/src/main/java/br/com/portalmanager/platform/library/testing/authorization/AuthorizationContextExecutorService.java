package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Wraps test executor tasks with the current authorization context and restores worker state. */
public final class AuthorizationContextExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final UserSession userSession;

    private AuthorizationContextExecutorService(ExecutorService delegate, UserSession userSession) {
        this.delegate = Objects.requireNonNull(delegate);
        this.userSession = Objects.requireNonNull(userSession);
    }

    public static AuthorizationContextExecutorService wrap(ExecutorService delegate) {
        UserSession session = UserContext.get()
                .orElseThrow(() -> new IllegalStateException(
                        "A user context must be established before wrapping the test executor"));
        return new AuthorizationContextExecutorService(delegate, session);
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command);
        delegate.execute(() -> {
            UserSession previous = UserContext.get().orElse(null);
            UserContext.set(userSession);
            try {
                command.run();
            } finally {
                UserContext.set(previous);
            }
        });
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}
