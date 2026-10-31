package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.aop.*;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.config.*;
import br.com.portalmanager.platform.library.authorization.exception.*;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import java.time.Instant;
import java.util.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EntryPolicySecurityTest {
    static class Facade {
        @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.CREATE,
            mechanism=AuthorizationMechanism.POLICY, policy="ingest")
        public void ingest(PolicyAuthorizationContext scope) { }
        @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.CREATE)
        public void token(AuthorizationContext scope) { }
    }
    final PlatformAuthorizationProperties properties = new PlatformAuthorizationProperties();
    final AuthorizationClient client = mock(AuthorizationClient.class);
    final ProceedingJoinPoint call = mock(ProceedingJoinPoint.class);
    final AuthorizationEntryOrigin origin = new AuthorizationEntryOrigin("Listener#receive", "configured", "https://sqs.example/123/physical", "event", "correlation", Instant.now());
    AuthorizationEntryOriginResolver resolver = () -> Optional.of(origin);
    final PolicyAuthorizationContext scope = new PolicyAuthorizationContext("correlation", "account", "app", "env");

    @BeforeEach void setup() throws Exception {
        properties.setTechnicalPrincipals(Set.of("audit-consumer"));
        properties.setEntryPolicies(Map.of("ingest", policy(Set.of(new AuthorizationEntryPolicy.Scope("account", "app", "env")))));
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(Facade.class.getMethod("ingest", PolicyAuthorizationContext.class));
        when(call.getSignature()).thenReturn(signature);
        when(call.getTarget()).thenReturn(new Facade());
        when(call.getArgs()).thenReturn(new Object[]{scope});
    }
    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); UserContext.clear(); MDC.clear(); }
    AuthorizationEntryPolicy policy(Set<AuthorizationEntryPolicy.Scope> scopes) {
        return new AuthorizationEntryPolicy(origin.entryPoint(), Facade.class.getName()+"#ingest", origin.destination(), origin.queueUrl(),
            "audit-consumer", AuthorizationAction.CREATE, AuthorizationLevel.ADM, scopes);
    }
    AuthorizationRequired required() throws Exception { return Facade.class.getMethod("ingest", PolicyAuthorizationContext.class).getAnnotation(AuthorizationRequired.class); }
    EntryPolicyAuthorizer authorizer() { return new EntryPolicyAuthorizer(properties, List.of(resolver)); }

    @Test void realAndMockPolicyUseSameExplicitPrincipalAndRestoreContext() throws Throwable {
        for (AuthorizationExecutionMode mode : AuthorizationExecutionMode.values()) {
            properties.setMode(mode);
            UserSession previous = new UserSession(); UserContext.set(previous); MDC.put("correlationId", "before");
            doAnswer(ignored -> {
                assertThat(UserContext.get().orElseThrow().getUserName()).isEqualTo("audit-consumer");
                assertThat(UserContext.get().orElseThrow().getAccountId()).isEqualTo("account");
                assertThat(UserContext.get().orElseThrow().isOwner()).isFalse();
                assertThat(MDC.get("correlationId")).isEqualTo("correlation"); return "ok";
            }).when(call).proceed();
            Object result = mode == AuthorizationExecutionMode.REAL
                ? new AuthorizationFacadeAspect(client, authorizer()).authorize(call, required())
                : new MockAuthorizationFacadeAspect(properties, authorizer()).authorize(call, required());
            assertThat(result).isEqualTo("ok");
            assertThat(UserContext.get()).containsSame(previous);
            assertThat(MDC.get("correlationId")).isEqualTo("before");
        }
        verifyNoInteractions(client);
    }
    @Test void payloadScopeCannotAuthorizeWithoutTransportProof() throws Throwable {
        resolver = Optional::empty;
        assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
        verify(call, never()).proceed();
    }
    @Test void httpCannotBorrowTrustedOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization-Mechanism", "POLICY");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
    }
    @Test void unknownPrincipalDenied() {
        properties.setTechnicalPrincipals(Set.of());
        assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
    }
    @Test void missingPolicyDenied() {
        properties.setEntryPolicies(Map.of());
        assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
    }
    @Test void accountApplicationAndEnvironmentAreExactTuples() {
        for (PolicyAuthorizationContext denied : List.of(new PolicyAuthorizationContext("correlation","other","app","env"),
            new PolicyAuthorizationContext("correlation","account","other","env"),new PolicyAuthorizationContext("correlation","account","app","other"))) {
            when(call.getArgs()).thenReturn(new Object[]{denied});
            assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
        }
    }
    @Test void wrongActionAndFacadeDenied() {
        AuthorizationEntryPolicy policy = policy(Set.of(new AuthorizationEntryPolicy.Scope("account","app","env")));
        properties.setEntryPolicies(Map.of("ingest", new AuthorizationEntryPolicy(policy.entryPoint(), policy.facade(), policy.destination(), policy.queueUrl(),
            policy.principal(), AuthorizationAction.DELETE, policy.level(), policy.scopes())));
        assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
        properties.setEntryPolicies(Map.of("ingest", new AuthorizationEntryPolicy(policy.entryPoint(), "Other#method", policy.destination(), policy.queueUrl(),
            policy.principal(), policy.action(), policy.level(), policy.scopes())));
        assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
    }
    @Test void anotherQueueOrEntrypointCannotBorrowPolicy() {
        for (AuthorizationEntryOrigin denied : List.of(
            new AuthorizationEntryOrigin("Other#receive",origin.destination(),origin.queueUrl(),"id","correlation",Instant.now()),
            new AuthorizationEntryOrigin(origin.entryPoint(),"other",origin.queueUrl(),"id","correlation",Instant.now()),
            new AuthorizationEntryOrigin(origin.entryPoint(),origin.destination(),"https://other/queue","id","correlation",Instant.now()))) {
            resolver=()->Optional.of(denied);
            assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
        }
    }
    @Test void absentOrMismatchedCorrelationDenied() {
        for (String correlation : Arrays.asList(null,"","different","injected\nline")) {
            when(call.getArgs()).thenReturn(new Object[]{new PolicyAuthorizationContext(correlation,"account","app","env")});
            assertThatThrownBy(() -> authorizer().authorize(call, required())).isInstanceOf(ForbiddenAccessException.class);
        }
    }
    @Test void failureRestoresSessionAndMdc() throws Throwable {
        UserSession previous=new UserSession(); UserContext.set(previous); MDC.put("correlationId","before");
        when(call.proceed()).thenThrow(new IllegalStateException("test"));
        assertThatThrownBy(() -> authorizer().authorize(call,required())).isInstanceOf(IllegalStateException.class);
        assertThat(UserContext.get()).containsSame(previous); assertThat(MDC.get("correlationId")).isEqualTo("before");
    }
    @Test void tokenMissingIsNotPromotedToPolicy() throws Throwable {
        AuthorizationRequired token = Facade.class.getMethod("token",AuthorizationContext.class).getAnnotation(AuthorizationRequired.class);
        when(call.getArgs()).thenReturn(new Object[]{new AuthorizationContext("correlation",null,null,null,null)});
        assertThatThrownBy(() -> new AuthorizationFacadeAspect(client,authorizer()).authorize(call,token)).isInstanceOf(UnauthorizedAccessException.class);
        verifyNoInteractions(client);
    }
    @Test void ambiguousSourcesRejectedAtStartup() {
        assertThatThrownBy(() -> new EntryPolicyAuthorizer(properties,List.of(resolver,resolver)).afterSingletonsInstantiated())
            .isInstanceOf(PlatformConfigurationException.class);
    }
    @Test void malformedOrUnboundPolicyRejectedAtStartup() {
        EntryPolicyConfigurationValidator validator=new EntryPolicyConfigurationValidator(properties);
        assertThatThrownBy(validator::afterSingletonsInstantiated).isInstanceOf(PlatformConfigurationException.class);
        validator.postProcessAfterInitialization(new Facade(),"facade");
        assertThatCode(validator::afterSingletonsInstantiated).doesNotThrowAnyException();
        properties.setTechnicalPrincipals(Set.of());
        assertThatThrownBy(validator::afterSingletonsInstantiated).isInstanceOf(PlatformConfigurationException.class);
    }
}
