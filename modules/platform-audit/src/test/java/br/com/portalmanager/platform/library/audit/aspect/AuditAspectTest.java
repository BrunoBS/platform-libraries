package br.com.portalmanager.platform.library.audit.aspect;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import br.com.portalmanager.platform.library.audit.outbox.AuditBeforeSnapshotProvider;
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditAspectTest {

    private final PlatformAuditProperties properties = new PlatformAuditProperties();
    private final AuditAuthorizationContextResolver contextResolver = mock(AuditAuthorizationContextResolver.class);
    private final AuditOutboxStore outboxStore = mock(AuditOutboxStore.class);
    private final AuditBeforeSnapshotProvider beforeSnapshotProvider = mock(AuditBeforeSnapshotProvider.class);
    private final AuditEventFactory singleEventFactory =
            new AuditEventFactory(properties, contextResolver, new ObjectMapper());
    private final AuditAspect aspect = new AuditAspect(
            new AuditSnapshotCollector(beforeSnapshotProvider),
            new AuditInvocationEventFactory(singleEventFactory),
            outboxStore
    );

    @BeforeEach
    void startTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        properties.setServiceName("key-service");
        when(contextResolver.resolve()).thenReturn(
                new AuditContext("account-1", "application-1", "dev", "user-1", "trace-1"));
    }

    @AfterEach
    void clearTransaction() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldStoreOneSnapshotAndTypedMetadataForAnAfterAction() throws Throwable {
        aspect.audit(joinPoint("update", Map.of("identifier", "key-1", "name", "api.timeout")));

        var payload = org.mockito.ArgumentCaptor.forClass(tools.jackson.databind.JsonNode.class);
        var metadata = org.mockito.ArgumentCaptor.forClass(tools.jackson.databind.JsonNode.class);
        verify(outboxStore).append(payload.capture(), metadata.capture());
        assertThat(payload.getValue().get("name").asText()).isEqualTo("api.timeout");
        assertThat(metadata.getValue().get("service").asText()).isEqualTo("key-service");
        assertThat(metadata.getValue().get("resourceType").asText()).isEqualTo("KEY");
        assertThat(metadata.getValue().get("eventType").asText()).isEqualTo("UPDATED");
        assertThat(metadata.getValue().get("resourceIdentifier").asText()).isEqualTo("key-1");
        assertThat(metadata.getValue().get("occurredAt").asText()).isNotBlank();
    }

    @Test
    void shouldCapturePurgeSnapshotBeforeExecutingTheUseCase() throws Throwable {
        List<String> order = new ArrayList<>();
        when(beforeSnapshotProvider.capture(any(), any(), any())).thenAnswer(invocation -> {
            order.add("capture");
            return new ObjectMapper().valueToTree(Map.of("identifier", "key-1", "lifecycle", "INACTIVE"));
        });
        ProceedingJoinPoint joinPoint = joinPoint("purge", null);
        doAnswer(invocation -> {
            order.add("proceed");
            return null;
        }).when(joinPoint).proceed();

        aspect.audit(joinPoint);

        assertThat(order).containsExactly("capture", "proceed");
        verify(outboxStore).append(any(), any());
    }

    @Test
    void shouldWriteEveryEventWhenInvocationReturnsMoreThanOneHundredItems() throws Throwable {
        List<Map<String, String>> items = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            items.add(Map.of("identifier", "key-" + index));
        }

        aspect.audit(joinPoint("updateMany", items));

        verify(outboxStore, org.mockito.Mockito.times(101)).append(any(), any());
    }

    @Test
    void shouldWriteSnapshotLargerThanThePreviousSizeThreshold() throws Throwable {
        String largeValue = "x".repeat(70_000);
        aspect.audit(joinPoint("update", Map.of("identifier", "key-1", "data", largeValue)));

        var payload = org.mockito.ArgumentCaptor.forClass(tools.jackson.databind.JsonNode.class);
        verify(outboxStore).append(payload.capture(), any());
        assertThat(payload.getValue().get("data").asText()).hasSize(70_000);
    }

    @Test
    void shouldNotWriteOutboxWhenUseCaseThrows() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("update", null);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("business failure"));

        assertThatThrownBy(() -> aspect.audit(joinPoint)).isInstanceOf(IllegalStateException.class);
        verify(outboxStore, never()).append(any(), any());
    }

    @Test
    void shouldRequireAnActiveTransactionBeforeRunningBusinessCode() throws Throwable {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        ProceedingJoinPoint joinPoint = joinPoint("update", Map.of("identifier", "key-1"));

        assertThatThrownBy(() -> aspect.audit(joinPoint)).isInstanceOf(AuditException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void shouldValidateWholeBatchBeforeWritingAnyOutboxRecord() throws Throwable {
        assertThatThrownBy(() -> aspect.audit(joinPoint("updateMany", List.of(
                Map.of("identifier", "key-1"), Map.of("name", "missing identifier")))))
                .isInstanceOf(AuditException.class);

        verify(outboxStore, never()).append(any(), any());
    }

    @Test
    void shouldPersistEveryExplicitBusinessFactFromRepeatedAnnotations() throws Throwable {
        aspect.audit(joinPoint("activate", Map.of("identifier", "key-1")));

        var metadata = org.mockito.ArgumentCaptor.forClass(tools.jackson.databind.JsonNode.class);
        org.mockito.Mockito.verify(outboxStore, org.mockito.Mockito.times(2)).append(any(), metadata.capture());
        assertThat(metadata.getAllValues())
                .extracting(node -> node.get("eventType").asText())
                .containsExactly("UPDATED", "ACTIVATED");
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object result) throws Throwable {
        Method method = TestUseCase.class.getDeclaredMethod(methodName);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(result);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestUseCase());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        return joinPoint;
    }

    static class TestUseCase {
        @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "KEY")
        void update() { }

        @Auditable(action = AuditAction.PURGE, event = "PURGED", resourceType = "KEY")
        void purge() { }

        @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "KEY")
        void updateMany() { }

        @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "KEY")
        @Auditable(action = AuditAction.ACTIVATE, event = "ACTIVATED", resourceType = "KEY")
        void activate() { }
    }
}
