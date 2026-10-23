package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Isolates the static user context in tests that do not start Spring. */
public final class UserContextIsolationExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(UserContextIsolationExtension.class);
    private static final String PREVIOUS_SESSION = "previous-session";

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NAMESPACE).put(PREVIOUS_SESSION,
                new PreviousSession(UserContext.get().orElse(null)));
        UserContext.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        PreviousSession previous = context.getStore(NAMESPACE)
                .remove(PREVIOUS_SESSION, PreviousSession.class);
        if (previous != null) {
            UserContext.set(previous.session());
        }
    }

    record PreviousSession(UserSession session) {}
}
