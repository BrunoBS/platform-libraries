package br.com.portalmanager.platform.library.authorization.registry;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationPolicy;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationMetadataRegistryTest {
    static class BaseController { public void findAll() {} }

    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    static class SampleController extends BaseController {
        @AuthorizationRequired(level = AuthorizationLevel.ADM)
        public void securedMethod() {}

        @AuthorizationRequired(level = AuthorizationLevel.DEV,
                workspacePathVariable = "tenantId",
                applicationPathVariable = "appId",
                environmentPathVariable = "envId")
        public void securedWithCustomPathVariables() {}
    }

    private final AuthorizationMetadataRegistry registry = new AuthorizationMetadataRegistry();

    @Test
    void shouldPreferMethodAnnotation() throws NoSuchMethodException {
        AuthorizationPolicy policy = registry.resolve(SampleController.class,
                SampleController.class.getMethod("securedMethod"), "POST");
        assertEquals(AuthorizationLevel.ADM, policy.level());
        assertEquals(AuthorizationPolicy.Source.METHOD, policy.source());
    }

    @Test
    void shouldUseClassAnnotationWhenMethodHasNoOverride() throws NoSuchMethodException {
        AuthorizationPolicy policy = registry.resolve(SampleController.class,
                BaseController.class.getMethod("findAll"), "GET");
        assertEquals(AuthorizationLevel.DEV, policy.level());
        assertEquals(AuthorizationPolicy.Source.CLASS, policy.source());
    }

    @Test
    void shouldResolveCustomPathVariableNames() throws NoSuchMethodException {
        AuthorizationPolicy policy = registry.resolve(SampleController.class,
                SampleController.class.getMethod("securedWithCustomPathVariables"), "GET");
        assertEquals("tenantId", policy.workspacePathVariable());
        assertEquals("appId", policy.applicationPathVariable());
        assertEquals("envId", policy.environmentPathVariable());
    }
}
