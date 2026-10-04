package br.com.portalmanager.platform.library.audit.event;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditFieldResolverTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final AuditFieldResolver resolver = new AuditFieldResolver(new ObjectMapper(), request);

    @Test
    void shouldResolvePathVariableUsingItsDeclaredName() throws Exception {
        ProceedingJoinPoint joinPoint = joinPoint("update", "account-7", Map.of("status", "ACTIVE"));

        Object value = resolver.resolve(joinPoint, null, annotation().resourceId());

        assertThat(value).isEqualTo("account-7");
    }

    @Test
    void shouldResolveRequestBodyResponseAndHeaderFields() throws Exception {
        ProceedingJoinPoint joinPoint = joinPoint("update", "account-7", Map.of("status", "ACTIVE"));
        when(request.getHeader("correlation-id")).thenReturn("trace-9");

        Object bodyValue = resolver.resolve(joinPoint, null, annotation().payload()[0]);
        Object responseValue = resolver.resolve(
                joinPoint,
                Map.of("name", "Operations"),
                annotation().payload()[1]
        );
        Object headerValue = resolver.resolve(joinPoint, null, annotation().payload()[2]);

        assertThat(bodyValue).isEqualTo("ACTIVE");
        assertThat(responseValue).isEqualTo("Operations");
        assertThat(headerValue).isEqualTo("trace-9");
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object... arguments) throws Exception {
        Method method = TestEndpoint.class.getDeclaredMethod(
                methodName,
                String.class,
                Map.class
        );
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"arg0", "arg1"});

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(arguments);
        return joinPoint;
    }

    private Auditable annotation() throws Exception {
        return TestEndpoint.class
                .getDeclaredMethod("update", String.class, Map.class)
                .getAnnotation(Auditable.class);
    }

    static class TestEndpoint {
        @Auditable(
                resource = "account",
                action = "UPDATE",
                resourceId = @AuditField(source = AuditFieldSource.PATH, field = "accountIdentifier"),
                payload = {
                        @AuditField(source = AuditFieldSource.BODY, field = "status"),
                        @AuditField(source = AuditFieldSource.RESPONSE, field = "name"),
                        @AuditField(source = AuditFieldSource.HEADER, field = "correlation-id")
                }
        )
        void update(
                @PathVariable("accountIdentifier") String id,
                @RequestBody Map<String, Object> body
        ) {
        }
    }
}
