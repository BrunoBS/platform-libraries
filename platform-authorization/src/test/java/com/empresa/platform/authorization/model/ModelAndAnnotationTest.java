package com.empresa.platform.authorization.model;

import com.empresa.platform.authorization.annotation.AuthorizationAccessPolicy;
import com.empresa.platform.authorization.annotation.AuthorizationRequired;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModelAndAnnotationTest {

    @Test
    void testAuthorizationPolicyAndLevel() {
        AuthorizationPolicy policy = AuthorizationPolicy.open();
        assertEquals(AuthorizationLevel.OPEN, policy.level());
        assertEquals(AuthorizationPolicy.Source.DEFAULT, policy.source());
        assertTrue(policy.toString().contains("OPEN"));
    }

    @Test
    void testParsedGroupRecord() {
        ParsedGroup group = new ParsedGroup("full", "profile", "env", "auth-suffix");
        assertEquals("full", group.fullGroup());
        assertEquals("auth-suffix", group.authorizer());
    }

    @Test
    void testUserSessionLogic() {
        UserSession session = new UserSession();
        session.setUserName("bruno");
        session.setGroups(Set.of("PM5_OWNER", "USER"));

        ParsedGroup parsed = new ParsedGroup("full", "prof", "env", "PAYMENT_SERVICE");
        session.setAuthorizerGroups(Set.of(parsed));

        assertTrue(session.isOwner());
        assertTrue(session.hasGroup("USER"));
        assertFalse(session.hasGroup("ADMIN"));

        assertTrue(session.hasAuthorizer("payment_service"));
        assertFalse(session.hasAuthorizer("unknown"));
        assertFalse(session.hasAuthorizer(null));

        assertNotNull(session.getExpirationFormatted());
    }

    @Test
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    void testAnnotationPresence() throws NoSuchMethodException {
        var method = this.getClass().getDeclaredMethod("testAnnotationPresence");
        var ann = method.getAnnotation(AuthorizationRequired.class);
        assertNotNull(ann);
        assertEquals(AuthorizationLevel.ADM, ann.level());
    }

    @Test
    void testClassAuthorizationAccessPolicyAnnotation() {
        var ann = PolicySample.class.getAnnotation(AuthorizationAccessPolicy.class);

        assertNotNull(ann);
        assertEquals(AuthorizationLevel.OPEN, ann.read());
        assertEquals(AuthorizationLevel.OWNER, ann.write());
    }

    @AuthorizationAccessPolicy(
            read = AuthorizationLevel.OPEN,
            write = AuthorizationLevel.OWNER
    )
    static class PolicySample {
    }
}
