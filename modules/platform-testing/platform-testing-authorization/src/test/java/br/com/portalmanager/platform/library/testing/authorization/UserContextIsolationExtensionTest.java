package br.com.portalmanager.platform.library.testing.authorization;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class UserContextIsolationExtensionTest {

    private final UserContextIsolationExtension extension = new UserContextIsolationExtension();

    @Test
    void clearsAndRestoresPreviousSession() {
        UserSession previous = new UserSession();
        UserSession testSession = new UserSession();
        ExtensionContext context = mock(ExtensionContext.class);
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);
        when(context.getStore(org.mockito.ArgumentMatchers.any())).thenReturn(store);
        AtomicReference<UserContextIsolationExtension.PreviousSession> saved =
                new AtomicReference<>();
        doAnswer(invocation -> {
            saved.set(invocation.getArgument(1));
            return null;
        }).when(store).put(org.mockito.ArgumentMatchers.eq("previous-session"),
                org.mockito.ArgumentMatchers.any());
        when(store.remove("previous-session", UserContextIsolationExtensionTest.previousType()))
                .thenAnswer(invocation -> saved.get());

        UserContext.set(previous);
        try {
            extension.beforeEach(context);
            assertThat(UserContext.get()).isEmpty();
            UserContext.set(testSession);
            extension.afterEach(context);
            assertThat(UserContext.get()).contains(previous);
        } finally {
            UserContext.clear();
        }
    }

    private static Class<UserContextIsolationExtension.PreviousSession> previousType() {
        return UserContextIsolationExtension.PreviousSession.class;
    }
}
