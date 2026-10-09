package br.com.portalmanager.platform.library.authorization.annotation;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

class AuthorizationFacadeContractTest {
    @Test void authorizationRequiredIsOnlyAllowedOnMethods() {
        assertThat(AuthorizationRequired.class.getAnnotation(Target.class).value())
                .containsExactly(ElementType.METHOD);
    }

    @Test void levelAndActionMustBeExplicit() throws Exception {
        assertThat(AuthorizationRequired.class.getMethod("level").getReturnType())
                .isEqualTo(AuthorizationLevel.class);
        assertThat(AuthorizationRequired.class.getMethod("action").getReturnType())
                .isEqualTo(AuthorizationAction.class);
        assertThat(AuthorizationRequired.class.getMethod("level").getDefaultValue()).isNull();
        assertThat(AuthorizationRequired.class.getMethod("action").getDefaultValue()).isNull();
    }

    @Test void everyPublicFacadeMethodMustDeclareAuthorization() {
        assertFacadeContract(ExampleFacade.class);
    }

    private static void assertFacadeContract(Class<?> facade) {
        assertThat(facade.getDeclaredMethods())
                .filteredOn(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
                .allSatisfy(method -> {
                    AuthorizationRequired required = method.getDeclaredAnnotation(AuthorizationRequired.class);
                    assertThat(required)
                            .as("%s.%s must explicitly declare @AuthorizationRequired", facade.getSimpleName(), method.getName())
                            .isNotNull();
                    assertThat(required.level()).isNotNull();
                    assertThat(required.action()).isNotNull();
                });
        assertThat(facade.isAnnotationPresent(AuthorizationRequired.class)).isFalse();
    }

    static class ExampleFacade {
        @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
        public void find() { }

        @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.CREATE)
        public void create() { }

        private void internal() { }
    }
}
