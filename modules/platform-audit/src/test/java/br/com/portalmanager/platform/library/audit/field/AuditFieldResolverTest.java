package br.com.portalmanager.platform.library.audit.field;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditFieldResolverTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final PlatformAuditProperties properties = new PlatformAuditProperties();
    private final AuditFieldResolver resolver =
            new AuditFieldResolver(properties, new ObjectMapper(), request);

    @Test
    void shouldResolvePathVariableUsingItsDeclaredName() throws Exception {
        var resolved = resolver.resolve(method(), new Object[]{"account-7", Map.of("status", "ACTIVE")},
                null, annotation());
        assertThat(resolved.resourceId()).isEqualTo("account-7");
    }

    @Test
    void shouldResolveRequestBodyResponseAndAllowedHeaderFields() throws Exception {
        when(request.getHeader("correlation-id")).thenReturn("trace-9");

        var resolved = resolver.resolve(method(), new Object[]{"account-7", Map.of("status", "ACTIVE")},
                Map.of("name", "Operations"), annotation());

        assertThat(resolved.payload()).isEqualTo(Map.of(
                "status", "ACTIVE",
                "name", "Operations",
                "correlation-id", "trace-9"
        ));
    }

    @Test
    void shouldRejectSensitiveFieldsEvenWhenExplicitlyConfigured() throws Exception {
        assertThatThrownBy(() -> resolver.resolve(
                method(),
                new Object[]{"account-7", Map.of("password", "secret")},
                Map.of(),
                sensitiveAnnotation()
        )).isInstanceOf(AuditException.class);
    }

    @Test
    void shouldRejectHeadersOutsideTheExplicitAllowlist() throws Exception {
        assertThatThrownBy(() -> resolver.resolve(
                method(),
                new Object[]{"account-7", Map.of()},
                Map.of(),
                authorizationAnnotation()
        )).isInstanceOf(AuditException.class);
    }

    private Method method() throws Exception {
        return TestEndpoint.class.getDeclaredMethod("update", String.class, Map.class);
    }

    private Auditable annotation() throws Exception {
        return method().getAnnotation(Auditable.class);
    }

    private Auditable sensitiveAnnotation() throws Exception {
        return TestEndpoint.class.getDeclaredMethod("password", String.class)
                .getAnnotation(Auditable.class);
    }

    private Auditable authorizationAnnotation() throws Exception {
        return TestEndpoint.class.getDeclaredMethod("authorization", String.class)
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

        @Auditable(
                resource = "account",
                action = "UPDATE",
                resourceId = @AuditField(source = AuditFieldSource.PATH, field = "id"),
                payload = @AuditField(source = AuditFieldSource.BODY, field = "password")
        )
        void password(@PathVariable("id") String id) {
        }

        @Auditable(
                resource = "account",
                action = "UPDATE",
                resourceId = @AuditField(source = AuditFieldSource.PATH, field = "id"),
                payload = @AuditField(source = AuditFieldSource.HEADER, field = "x-request-id")
        )
        void authorization(@PathVariable("id") String id) {
        }
    }
}
