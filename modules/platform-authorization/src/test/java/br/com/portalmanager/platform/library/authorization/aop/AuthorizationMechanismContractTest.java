package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.config.AuthorizationExecutionMode;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationMechanism;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationMechanismContractTest {
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public void token() { }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE,
            mechanism = AuthorizationMechanism.POLICY)
    public void policy() { }

    @Test void tokenIsTheDefaultAndExecutionValuesAreUnchanged() throws Exception {
        assertThat(required("token").mechanism()).isEqualTo(AuthorizationMechanism.TOKEN);
        assertThat(required("policy").mechanism()).isEqualTo(AuthorizationMechanism.POLICY);
        assertThat(AuthorizationExecutionMode.values()).containsExactly(AuthorizationExecutionMode.REAL, AuthorizationExecutionMode.MOCK);
        assertThat(AuthorizationMechanism.values()).containsExactly(AuthorizationMechanism.TOKEN, AuthorizationMechanism.POLICY);
        assertThat(new PlatformAuthorizationProperties().getMode()).isEqualTo(AuthorizationExecutionMode.REAL);
    }

    @Test void existingModePropertyBindsToRenamedType() {
        for (AuthorizationExecutionMode mode : AuthorizationExecutionMode.values()) {
            Binder binder = new Binder(new MapConfigurationPropertySource(Map.of("platform.authorization.mode", mode.name())));
            PlatformAuthorizationProperties properties = binder.bind("platform.authorization", Bindable.of(PlatformAuthorizationProperties.class)).get();
            assertThat(properties.getMode()).isEqualTo(mode);
        }
    }

    @Test void realPolicyWithoutTrustedEvaluatorIsDeniedBeforeClientOrInvocation() {
        AuthorizationClient client = mock(AuthorizationClient.class);
        ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
        assertThatThrownBy(() -> new AuthorizationFacadeAspect(client).authorize(invocation, required("policy")))
                .isInstanceOf(ForbiddenAccessException.class);
        verifyNoInteractions(client, invocation);
    }

    @Test void mockDoesNotImplicitlyAuthorizePolicy() {
        ProceedingJoinPoint invocation = mock(ProceedingJoinPoint.class);
        PlatformAuthorizationProperties properties = new PlatformAuthorizationProperties();
        properties.setMode(AuthorizationExecutionMode.MOCK);
        assertThatThrownBy(() -> new MockAuthorizationFacadeAspect(properties).authorize(invocation, required("policy")))
                .isInstanceOf(ForbiddenAccessException.class);
        verifyNoInteractions(invocation);
    }

    private AuthorizationRequired required(String method) throws NoSuchMethodException {
        return getClass().getMethod(method).getAnnotation(AuthorizationRequired.class);
    }
}
