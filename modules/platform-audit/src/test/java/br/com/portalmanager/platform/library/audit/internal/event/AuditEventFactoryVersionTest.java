package br.com.portalmanager.platform.library.audit.internal.event;

import br.com.portalmanager.platform.library.audit.api.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditAction;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditContext;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditMetadata;
import br.com.portalmanager.platform.library.audit.internal.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.internal.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditEventFactoryVersionTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @Auditable(action = AuditAction.CREATE, event = "CREATED", resourceType = "WORKSPACE")
    public void create() { }
    @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "WORKSPACE")
    public void update() { }

    private AuditEventFactory.CapturedAuditEvent capture(String operation, String snapshot) throws Exception {
        PlatformAuditProperties properties = new PlatformAuditProperties();
        properties.setServiceName("workspace-service");
        AuditAuthorizationContextResolver resolver = mock(AuditAuthorizationContextResolver.class);
        when(resolver.resolve()).thenReturn(new AuditContext("account", null, null, "actor", "correlation"));
        Auditable annotation = getClass().getMethod(operation).getAnnotation(Auditable.class);
        return new AuditEventFactory(properties, resolver, json).create(annotation, json.readTree(snapshot));
    }

    @Test void creationCapturesAssignedIdentifierAndZeroVersionWithoutChangingSnapshot() throws Exception {
        String snapshot = "{\"identifier\":\"created-resource\",\"version\":0}";
        AuditEventFactory.CapturedAuditEvent event = capture("create", snapshot);
        assertThat(event.metadata().path("resourceIdentifier").asText()).isEqualTo("created-resource");
        assertThat(event.metadata().path("resourceVersion").longValue()).isZero();
        assertThat(event.payload()).isEqualTo(json.readTree(snapshot));
    }

    @Test void updateCapturesReturnedVersionWithoutIntTruncation() throws Exception {
        AuditEventFactory.CapturedAuditEvent event = capture("update", "{\"id\":\"updated-resource\",\"version\":9223372036854775807}");
        assertThat(event.metadata().path("resourceIdentifier").asText()).isEqualTo("updated-resource");
        assertThat(event.metadata().path("resourceVersion").longValue()).isEqualTo(Long.MAX_VALUE);
    }

    @Test void missingOrNullVersionRemainsNull() throws Exception {
        for (String snapshot : new String[]{"{\"id\":\"resource\"}", "{\"id\":\"resource\",\"version\":null}"}) {
            JsonNode metadata = capture("create", snapshot).metadata();
            assertThat(metadata.has("resourceVersion")).isTrue();
            assertThat(metadata.get("resourceVersion").isNull()).isTrue();
        }
    }

    @Test void invalidVersionsAreRejectedRatherThanCoercedOrTruncated() {
        for (String version : new String[]{"\"7\"", "true", "1.5", "9223372036854775808"}) {
            assertThatThrownBy(() -> capture("update", "{\"id\":\"resource\",\"version\":" + version + "}"))
                    .isInstanceOf(ValidationException.class).hasMessage(AuditMessageKeys.RESOURCE_VERSION_INVALID);
        }
    }

    @Test void originalMetadataConstructorRemainsAvailable() {
        AuditMetadata metadata = new AuditMetadata("service", "resource", "CREATED", "id",
                null, null, null, null, null, Instant.EPOCH);
        assertThat(metadata.resourceVersion()).isNull();
    }
}
