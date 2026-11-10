package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.portalmanager.platform.library.messaging.api.exception.ApiException;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisConfigurationProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisParameterReader;
import br.com.portalmanager.platform.library.parameterclient.internal.serialization.ExactDocument;
import br.com.portalmanager.platform.library.redis.api.store.RedisStore;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConfigurationProviderTest {
    final JsonMapper mapper = JsonMapper.builder().build();
    final RedisStore store = mock(RedisStore.class);
    final RedisStore scoped = mock(RedisStore.class);
    { when(store.withNamespace("dynamic-parameter")).thenReturn(scoped); }
    final ConfigurationProvider provider = new RedisConfigurationProvider(new RedisParameterReader(store, mapper));

    void value(String json) {
        ExactDocument exact = readExact(json);
        when(scoped.get("platform.value", ExactDocument.class)).thenReturn(Optional.of(exact));
    }

    private ExactDocument readExact(String json) {
        try {
            return mapper.readValue(json, ExactDocument.class);
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    @Test
    void readsAllDocumentedKindsAndTypedNullDefaults() {
        value("{\"type\":\"CONFIGURATION\",\"value\":30}");
        assertThat(provider.get("platform.value", 10)).isEqualTo(30);
        assertThat(provider.get("platform.value", int.class, 10)).isEqualTo(30);
        value("{\"type\":\"CONFIGURATION\",\"value\":true}");
        assertThat(provider.get("platform.value", false)).isTrue();
        value("{\"type\":\"CONFIGURATION\",\"value\":\"STANDARD\"}");
        assertThat(provider.get("platform.value", "fallback")).isEqualTo("STANDARD");
        assertThat(provider.get("platform.value", Mode.class, null)).isEqualTo(Mode.STANDARD);
        value("{\"type\":\"CONFIGURATION\",\"value\":{\"days\":30}}");
        assertThat(provider.get("platform.value", Settings.class, new Settings(1))).isEqualTo(new Settings(30));
        value("{\"type\":\"CONFIGURATION\",\"value\":[\"BR\",\"US\"]}");
        assertThat(provider.get("platform.value", new TypeReference<List<String>>() {}, List.of()))
                .containsExactly("BR", "US");
        value("{\"type\":\"CONFIGURATION\",\"value\":0.00999999999999999999}");
        assertThat(provider.get("platform.value", java.math.BigDecimal.class, null))
                .isEqualByComparingTo("0.00999999999999999999");
    }

    @Test
    void documentedConsumerCallsAreExecutableWithoutFeatureInference() {
        Map<String, String> values = Map.of(
                "key-service.quarantineDays", "30",
                "platform.auditEnabled", "true",
                "platform.operationMode", "\"STANDARD\"",
                "platform.settings", "{\"maxAttempts\":5}",
                "platform.regions", "[\"BR\",\"US\"]");
        values.forEach((key, json) -> when(scoped.get(key, ExactDocument.class)).thenReturn(
                Optional.of(readExact("{\"type\":\"CONFIGURATION\",\"value\":" + json + "}"))));
        int days = provider.get("key-service.quarantineDays", 30);
        boolean audit = provider.get("platform.auditEnabled", false);
        String mode = provider.get("platform.operationMode", "STANDARD");
        MySettings settings = provider.get("platform.settings", MySettings.class, new MySettings());
        List<String> regions = provider.get("platform.regions", new TypeReference<List<String>>() {}, List.of());
        assertThat(days).isEqualTo(30);
        assertThat(audit).isTrue();
        assertThat(mode).isEqualTo("STANDARD");
        assertThat(settings).isEqualTo(new MySettings(5));
        assertThat(regions).containsExactly("BR", "US");
        verify(scoped, never()).get("orders.auditEnabled", ExactDocument.class);
    }

    @Test
    void absenceReturnsTheExactDefaultWithoutFallbackLookup() {
        Settings fallback = new Settings(30);
        assertThat(provider.get("missing.settings", Settings.class, fallback)).isSameAs(fallback);
        assertThat(provider.get("missing.settings", Settings.class, null)).isNull();
        List<String> list = List.of("BR");
        assertThat(provider.get("missing.regions", new TypeReference<List<String>>() {}, list)).isSameAs(list);
        assertThat(provider.get("other.auditEnabled", false)).isFalse();
        verify(scoped, times(2)).get("missing.settings", ExactDocument.class);
        verify(scoped).get("missing.regions", ExactDocument.class);
        verify(scoped).get("other.auditEnabled", ExactDocument.class);
        verify(store).withNamespace("dynamic-parameter");
        verifyNoMoreInteractions(store);
    }

    @Test
    void rejectsAmbiguousInferenceAndInvalidInputsBeforeReading() {
        for (String key : new String[]{null, "", " ", "\t\n"}) {
            assertThatThrownBy(() -> provider.get(key, 1)).hasMessage("parameter-client.validation");
        }
        assertThatThrownBy(() -> provider.get("platform.value", (Object) null)).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.get("platform.value", List.of())).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.get("platform.value", new Settings(1))).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.get("platform.value", (Class<String>) null, null)).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.get("platform.value", (TypeReference<String>) null, null)).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.get("platform.value", List.class, null)).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.get("platform.value", new TypeReference<List<?>>() {}, null)).hasMessage("parameter-client.validation");
        assertThatThrownBy(() -> provider.get("platform.value", Object.class, null)).hasMessage("parameter-client.validation");
        verify(store).withNamespace("dynamic-parameter");
        verifyNoInteractions(scoped);
        verifyNoMoreInteractions(store);
    }

    @Test
    void readsNonBlankKeysExactlyAsProvidedWithoutFormatValidation() {
        ExactDocument envelope = readExact("{\"type\":\"CONFIGURATION\",\"value\":30}");
        for (String key : List.of("single", "rules..value", "config:platform.value",
                "dynamic-parameter:platform.value", " platform.value ")) {
            when(scoped.get(key, ExactDocument.class)).thenReturn(Optional.of(envelope));
            assertThat(provider.get(key, 1)).isEqualTo(30);
            verify(scoped).get(key, ExactDocument.class);
        }
    }

    @Test
    void incompatibleAndNullPayloadsAreErrorsNotDefaults() {
        for (String json : List.of(
                "{\"type\":\"CONFIGURATION\",\"value\":\"30\"}",
                "{\"type\":\"CONFIGURATION\",\"value\":30.5}",
                "{\"type\":\"CONFIGURATION\",\"value\":null}",
                "{\"type\":\"CONFIGURATION\"}", "{}", "[]", "30",
                "{\"type\":\"UNKNOWN\",\"value\":30}")) {
            value(json);
            assertThatThrownBy(() -> provider.get("platform.value", 7))
                    .isInstanceOf(ApiException.class).hasMessage("parameter-client.value.invalid");
        }
        value("{\"type\":\"CONFIGURATION\",\"value\":[1]}");
        assertThatThrownBy(() -> provider.get("platform.value", new TypeReference<List<String>>() {}, List.of()))
                .hasMessage("parameter-client.value.invalid");
    }

    @Test
    void rejectsExpressionEnvelopeInsteadOfReturningItAsConfiguration() {
        value("{\"type\":\"EXPRESSION\",\"value\":{\"operator\":\"EQUALS\",\"valueType\":\"STRING\",\"value\":\"BR\"}}");
        assertThatThrownBy(() -> provider.get("platform.value", JsonNode.class, null))
                .hasMessage("parameter-client.value.invalid");
    }

    @Test
    void failuresAreControlledAndDoNotRetainSensitiveCauses() {
        for (String key : List.of("redis.unavailable", "redis.timeout", "redis.serialization", "redis.type")) {
            doThrow(new ApiException(key, new RuntimeException("SECRET_TOKEN_123")))
                    .when(scoped).get("platform.value", ExactDocument.class);
            Throwable failure = catchThrowable(() -> provider.get("platform.value", 7));
            String expected = switch(key) {
                case "redis.timeout" -> "parameter-client.timeout";
                case "redis.serialization", "redis.type" -> "parameter-client.value.invalid";
                default -> "parameter-client.read.failed";
            };
            assertThat(failure).hasMessage(expected);
            StringWriter trace = new StringWriter(); failure.printStackTrace(new PrintWriter(trace));
            assertThat(trace.toString()).doesNotContain("SECRET_TOKEN_123");
            assertThat(failure.getCause()).isNull();
        }
    }

    enum Mode { STANDARD }
    record Settings(int days) {}
    public record MySettings(int maxAttempts) { public MySettings() { this(3); } }
}
