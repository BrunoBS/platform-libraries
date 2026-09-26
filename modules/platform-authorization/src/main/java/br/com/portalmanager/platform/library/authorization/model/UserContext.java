package br.com.portalmanager.platform.library.authorization.model;

import br.com.portalmanager.platform.library.authorization.model.UserSession;
import java.util.Optional;

public final class UserContext {

    private static final ThreadLocal<UserSession> CONTEXT = new ThreadLocal<>();

    private UserContext() {}

    public static void set(UserSession session) {
        if (session == null) {
            clear();
        } else {
            CONTEXT.set(session);
        }
    }

    public static Optional<UserSession> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
