package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.event.AuditFieldResolver;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditAspectTest {

    private final AuditPublisher publisher = mock(AuditPublisher.class);
    private final AuditAuthorizationContextResolver contextResolver =
            mock(AuditAuthorizationContextResolver.class);
    private final PlatformAuditProperties properties = new PlatformAuditProperties();
    private final AuditAspect aspect = new AuditAspect(
            properties,
            new AuditEventFactory(
                    properties,
                    contextResolver,
                    new AuditFieldResolver(new ObjectMapper(), mock(HttpServletRequest.class))
            ),
            publisher
    );

    @Test
    void shouldPublishOnlyExplicitlyAllowlistedPayloadFields() throws Throwable {
        when(contextResolver.resolve()).thenReturn(
                new AuditContext("account-1", "application-1", "dev", "user-1", "trace-1"));

        aspect.audit(joinPoint(
                "update",
                ResponseEntity.ok(Map.of(
                        "id", "resource-1",
                        "name", "Account",
                        "secret", "must-not-be-published"
                ))
        ));

        var event = org.mockito.ArgumentCaptor.forClass(AuditEventRequest.class);
        verify(publisher).publish(event.capture());
        assertThat(event.getValue().resourceId()).isEqualTo("resource-1");
        assertThat(event.getValue().payload()).isEqualTo(Map.of("name", "Account"));
    }

    @Test
    void shouldNotIncludePayloadWhenNoFieldsAreAllowlisted() throws Throwable {
        when(contextResolver.resolve()).thenReturn(
                new AuditContext("account-1", "application-1", "dev", "user-1", "trace-1"));

        aspect.audit(joinPoint(
                "updateWithoutPayload",
                ResponseEntity.ok(Map.of("id", "resource-1", "secret", "must-not-be-published"))
        ));

        var event = org.mockito.ArgumentCaptor.forClass(AuditEventRequest.class);
        verify(publisher).publish(event.capture());
        assertThat(event.getValue().payload()).isEqualTo(Map.of());
    }

    @Test
    void shouldUseStandardAuditExceptionWhenResourceIdentifierIsMissing() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("update", ResponseEntity.ok(Map.of("name", "Account")));

        assertThatThrownBy(() -> aspect.audit(joinPoint))
                .isInstanceOf(AuditException.class);
        verify(publisher, times(0)).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSkipNonSuccessfulHttpResponses() throws Throwable {
        aspect.audit(joinPoint("update", ResponseEntity.badRequest().body(Map.of("id", "resource-1"))));

        verify(contextResolver, times(0)).resolve();
        verify(publisher, times(0)).publish(org.mockito.ArgumentMatchers.any());
    }


    @Test
    void shouldPropagateMissingContextWhenStrictModeIsEnabled() throws Throwable {
        when(contextResolver.resolve()).thenThrow(
                new AuditException("audit.context.user.missing")
        );

        assertThatThrownBy(() -> aspect.audit(joinPoint(
                "updateWithoutPayload",
                ResponseEntity.ok(Map.of("id", "resource-1"))
        ))).isInstanceOf(AuditException.class);

        verify(publisher, times(0)).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSkipEventWhenPermissiveModeIsEnabledAndContextIsMissing() throws Throwable {
        properties.setFailOnError(false);
        when(contextResolver.resolve()).thenThrow(
                new AuditException("audit.context.user.missing")
        );

        aspect.audit(joinPoint(
                "updateWithoutPayload",
                ResponseEntity.ok(Map.of("id", "resource-1"))
        ));

        verify(publisher, times(0)).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPublishOneEventForEachItemInACollection() throws Throwable {
        when(contextResolver.resolve()).thenReturn(
                new AuditContext("account-1", "application-1", "dev", "user-1", "trace-1"));

        aspect.audit(joinPoint(
                "updateWithoutPayload",
                ResponseEntity.ok(List.of(
                        Map.of("id", "resource-1"),
                        Map.of("id", "resource-2")
                ))
        ));

        var events = org.mockito.ArgumentCaptor.forClass(AuditEventRequest.class);
        verify(publisher, times(2)).publish(events.capture());
        assertThat(events.getAllValues())
                .extracting(AuditEventRequest::resourceId)
                .containsExactly("resource-1", "resource-2");
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object result) throws Throwable {
        Method method = TestController.class.getDeclaredMethod(methodName);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestController());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        return joinPoint;
    }

    static class TestController {
        @Auditable(
                resource = "account",
                action = "UPDATE",
                resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "id"),
                payload = @AuditField(source = AuditFieldSource.RESPONSE, field = "name")
        )
        void update() {
        }

        @Auditable(
                resource = "account",
                action = "UPDATE",
                resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "id")
        )
        void updateWithoutPayload() {
        }
    }
}
