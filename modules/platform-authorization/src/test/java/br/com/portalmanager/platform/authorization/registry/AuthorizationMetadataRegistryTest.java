package br.com.portalmanager.platform.authorization.registry;

import br.com.portalmanager.platform.authorization.annotation.AuthorizationAccessPolicy;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.authorization.model.AuthorizationPolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationMetadataRegistryTest {

    static class BaseController {
        public void findAll() {}
        public void create() {}
    }

    @AuthorizationAccessPolicy(
            read = AuthorizationLevel.OPEN,
            write = AuthorizationLevel.OWNER
    )
    static class SampleController extends BaseController {

        @AuthorizationRequired(level = AuthorizationLevel.ADM)
        public void securedMethod() {}
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    static class LegacyController {
        public void inheritedMethod() {}
    }

    private final AuthorizationMetadataRegistry registry =
            new AuthorizationMetadataRegistry();

    @Test
    void shouldPreferMethodAnnotation() throws NoSuchMethodException {
        Method method = SampleController.class.getMethod("securedMethod");

        AuthorizationPolicy policy =
                registry.resolve(SampleController.class, method, "POST");

        assertEquals(AuthorizationLevel.ADM, policy.level());
        assertEquals(AuthorizationPolicy.Source.METHOD, policy.source());
    }

    @Test
    void shouldApplyReadPolicyToInheritedGetMethod() throws NoSuchMethodException {
        Method method = BaseController.class.getMethod("findAll");

        AuthorizationPolicy policy =
                registry.resolve(SampleController.class, method, "GET");

        assertEquals(AuthorizationLevel.OPEN, policy.level());
        assertEquals(AuthorizationPolicy.Source.CLASS_POLICY, policy.source());
    }

    @Test
    void shouldApplyWritePolicyToInheritedPostMethod() throws NoSuchMethodException {
        Method method = BaseController.class.getMethod("create");

        AuthorizationPolicy policy =
                registry.resolve(SampleController.class, method, "POST");

        assertEquals(AuthorizationLevel.OWNER, policy.level());
        assertEquals(AuthorizationPolicy.Source.CLASS_POLICY, policy.source());
    }

    @Test
    void shouldKeepLegacyClassAuthorizationRequired() throws NoSuchMethodException {
        Method method = LegacyController.class.getMethod("inheritedMethod");

        AuthorizationPolicy policy =
                registry.resolve(LegacyController.class, method, "GET");

        assertEquals(AuthorizationLevel.DEV, policy.level());
        assertEquals(AuthorizationPolicy.Source.CLASS, policy.source());
    }
}
