package br.com.portalmanager.platform.library.audit.internal.event;

import br.com.portalmanager.platform.library.audit.api.annotation.AuditAttribute;
import br.com.portalmanager.platform.library.audit.api.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditAction;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditContext;
import br.com.portalmanager.platform.library.audit.internal.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.internal.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditAttributeMappingTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @Auditable(action = AuditAction.CREATE, event = "CREATED", resourceType = "WORKSPACE")
    public void defaults() { }

    @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "WORKSPACE", attributes = {
            @AuditAttribute(key = "resourceIdentifier", value = "code"),
            @AuditAttribute(key = "resourceVersion", value = "revision"),
            @AuditAttribute(key = "resourceStatus", value = "lifecycle"),
            @AuditAttribute(key = "enabled", value = "active"),
            @AuditAttribute(key = "extra", value = "details"),
            @AuditAttribute(key = "optional", value = "optional"),
            @AuditAttribute(key = "ignored", value = "absent")})
    public void custom() { }

    @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "WORKSPACE", attributes = {
            @AuditAttribute(key = "resourceIdentifier", value = "id")})
    public void idOnly() { }

    @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "WORKSPACE", attributes = {
            @AuditAttribute(key = "service", value = "name")})
    public void reserved() { }

    @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "WORKSPACE", attributes = {
            @AuditAttribute(key = "resourceIdentifier", value = "identifier"),
            @AuditAttribute(key = "resourceIdentifier", value = "id")})
    public void duplicate() { }

    @Auditable(action = AuditAction.UPDATE, event = "UPDATED", resourceType = "WORKSPACE", attributes = {
            @AuditAttribute(key = "resourceIdentifier", value = " ")})
    public void blank() { }

    private AuditEventFactory.CapturedAuditEvent capture(String method, String snapshot) throws Exception {
        PlatformAuditProperties properties = new PlatformAuditProperties();
        properties.setServiceName("workspace-service");
        AuditAuthorizationContextResolver context = mock(AuditAuthorizationContextResolver.class);
        when(context.resolve()).thenReturn(new AuditContext("account", null, null, "actor", "trace"));
        return new AuditEventFactory(properties, context, json).create(
                getClass().getMethod(method).getAnnotation(Auditable.class), json.readTree(snapshot));
    }

    @Test void defaultAnnotationUsesIdentifierAndVersion() throws Exception {
        AuditAttribute[] mappings = getClass().getMethod("defaults").getAnnotation(Auditable.class).attributes();
        assertThat(mappings).hasSize(2);
        assertThat(mappings[0].key()).isEqualTo("resourceIdentifier");
        assertThat(mappings[0].value()).isEqualTo("identifier");
        assertThat(mappings[1].key()).isEqualTo("resourceVersion");
        assertThat(mappings[1].value()).isEqualTo("version");
        JsonNode metadata = capture("defaults", "{\"identifier\":\"resource\",\"version\":0}").metadata();
        assertThat(metadata.path("resourceIdentifier").asText()).isEqualTo("resource");
        assertThat(metadata.path("resourceVersion").longValue()).isZero();
    }

    @Test void customMappingRenamesFieldsPreservesTypesAndSkipsMissingSources() throws Exception {
        String snapshot = "{\"identifier\":\"unused\",\"version\":99,\"code\":\"resource\",\"revision\":0,"
                + "\"lifecycle\":\"ACTIVE\",\"active\":false,\"details\":{\"nested\":[1,2]},\"optional\":null}";
        AuditEventFactory.CapturedAuditEvent captured = capture("custom", snapshot);
        JsonNode metadata = captured.metadata();
        assertThat(metadata.path("resourceIdentifier").asText()).isEqualTo("resource");
        assertThat(metadata.path("resourceVersion").longValue()).isZero();
        assertThat(metadata.path("resourceStatus").asText()).isEqualTo("ACTIVE");
        assertThat(metadata.path("enabled").isBoolean()).isTrue();
        assertThat(metadata.path("enabled").booleanValue()).isFalse();
        assertThat(metadata.path("extra").path("nested").isArray()).isTrue();
        assertThat(metadata.get("optional").isNull()).isTrue();
        assertThat(metadata.has("ignored")).isFalse();
        assertThat(captured.payload()).isEqualTo(json.readTree(snapshot));
    }

    @Test void explicitArrayReplacesRatherThanExtendsDefaults() throws Exception {
        JsonNode metadata = capture("idOnly", "{\"id\":42,\"identifier\":\"unused\",\"version\":7}").metadata();
        assertThat(metadata.path("resourceIdentifier").asText()).isEqualTo("42");
        assertThat(metadata.has("resourceVersion")).isFalse();
    }

    @Test void duplicateBlankAndReservedKeysAreRejected() {
        for (String method : new String[]{"reserved", "duplicate", "blank"}) {
            assertThatThrownBy(() -> capture(method, "{\"identifier\":\"resource\"}"))
                    .isInstanceOf(ValidationException.class).hasMessage(AuditMessageKeys.ATTRIBUTE_MAPPING_INVALID);
        }
    }

    @Test void missingResourceIdentityStillPreventsUploadingAnUnaddressableSnapshot() {
        assertThatThrownBy(() -> capture("custom", "{\"identifier\":\"unused-default\"}"))
                .isInstanceOf(ValidationException.class).hasMessage(AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING);
    }
}
