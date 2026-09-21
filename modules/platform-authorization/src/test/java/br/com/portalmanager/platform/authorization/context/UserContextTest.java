package br.com.portalmanager.platform.authorization.context;

import br.com.portalmanager.platform.authorization.model.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import br.com.portalmanager.platform.authorization.model.UserSession;
class UserContextTest {

    @BeforeEach
    void setUp() {
        UserContext.clear();
    }

    @Test
    void shouldManageThreadLocalContextCorrectly() {
        assertTrue(UserContext.get().isEmpty());

        UserSession session = new UserSession();
        session.setUserName("bruno");
        UserContext.set(session);

        assertTrue(UserContext.get().isPresent());
        assertEquals("bruno", UserContext.get().get().getUserName());

        UserContext.clear();
        assertTrue(UserContext.get().isEmpty());
    }

    @Test
    void shouldClearContextWhenSettingNull() {
        UserContext.set(new UserSession());
        UserContext.set(null);
        assertTrue(UserContext.get().isEmpty());
    }
}
