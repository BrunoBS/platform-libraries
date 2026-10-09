package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationMetadataRegistryTest {
    static class BaseController {
        public void findAll() {}
    }

    static class SampleController extends BaseController {
        @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
        public void securedMethod() {}

        @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
        public void securedWithContextIdentifiers() {}
    }

    private final AuthorizationMetadataRegistry registry = new AuthorizationMetadataRegistry();

    @Test
    void shouldResolveMethodAnnotation() throws NoSuchMethodException {
        AuthorizationPolicy policy = registry.resolve(SampleController.class,
                SampleController.class.getMethod("securedMethod"));
        assertEquals(AuthorizationLevel.ADM, policy.level());
        assertEquals(AuthorizationPolicy.Source.METHOD, policy.source());
    }

    @Test
    void shouldUseOpenPolicyForUnannotatedMethod() throws NoSuchMethodException {
        AuthorizationPolicy policy = registry.resolve(SampleController.class,
                BaseController.class.getMethod("findAll"));
        assertEquals(AuthorizationLevel.OPEN, policy.level());
        assertEquals(AuthorizationPolicy.Source.DEFAULT, policy.source());
    }

    @Test
    void shouldUseContextIdentifierNames() throws NoSuchMethodException {
        AuthorizationPolicy policy = registry.resolve(SampleController.class,
                SampleController.class.getMethod("securedWithContextIdentifiers"));
        assertEquals("workspaceIdentifier", policy.workspacePathVariable());
        assertEquals("applicationIdentifier", policy.applicationPathVariable());
        assertEquals("environmentIdentifier", policy.environmentPathVariable());
    }
}
