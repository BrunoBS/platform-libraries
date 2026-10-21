package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisParameterProvider;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ParameterProviderTest {
    final JsonMapper mapper = JsonMapper.builder().build();
    final RedisStore store = mock(RedisStore.class);
    final ParameterProvider provider = new RedisParameterProvider(store, new RedisJsonCodec(mapper, 1048576));

    void value(String json) {
        when(store.get("platform.value", JsonNode.class)).thenReturn(Optional.of(mapper.readTree(json)));
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
    }

    @Test
    void documentedConsumerCallsAreExecutableWithoutFeatureInference() {
        Map<String, String> values = Map.of(
                "key-service.quarantineDays", "30",
                "platform.auditEnabled", "true",
                "platform.operationMode", "\"STANDARD\"",
                "platform.settings", "{\"maxAttempts\":5}",
                "platform.regions", "[\"BR\",\"US\"]");
        values.forEach((key, json) -> when(store.get(key, JsonNode.class)).thenReturn(
                Optional.of(mapper.readTree("{\"type\":\"CONFIGURATION\",\"value\":" + json + "}"))));
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
        verify(store, never()).get("orders.auditEnabled", JsonNode.class);
    }

    @Test
    void absenceReturnsTheExactDefaultWithoutFallbackLookup() {
        var fallback = new Settings(30);
        assertThat(provider.get("missing.settings", Settings.class, fallback)).isSameAs(fallback);
        assertThat(provider.get("missing.settings", Settings.class, null)).isNull();
        var list = List.of("BR");
        assertThat(provider.get("missing.regions", new TypeReference<List<String>>() {}, list)).isSameAs(list);
        assertThat(provider.get("other.auditEnabled", false)).isFalse();
        verify(store, times(2)).get("missing.settings", JsonNode.class);
        verify(store).get("missing.regions", JsonNode.class);
        verify(store).get("other.auditEnabled", JsonNode.class);
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
        verifyNoInteractions(store);
    }

    @Test
    void readsNonBlankKeysExactlyAsProvidedWithoutFormatValidation() {
        JsonNode envelope = mapper.readTree("{\"type\":\"CONFIGURATION\",\"value\":30}");
        for (String key : List.of("single", "rules..value", "config:platform.value",
                "dynamic-parameter:platform.value", " platform.value ")) {
            when(store.get(key, JsonNode.class)).thenReturn(Optional.of(envelope));
            assertThat(provider.get(key, 1)).isEqualTo(30);
            verify(store).get(key, JsonNode.class);
        }
    }

    @Test
    void incompatibleAndNullPayloadsAreErrorsNotDefaults() {
        for (String json : List.of(
                "{\"type\":\"CONFIGURATION\",\"value\":\"30\"}",
                "{\"type\":\"CONFIGURATION\",\"value\":30.5}",
                "{\"type\":\"CONFIGURATION\",\"value\":null}",
                "{\"type\":\"CONFIGURATION\"}", "{}", "null", "[]", "30",
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
    void expressionIsReturnedAsDataAndNeverEvaluated() {
        value("{\"type\":\"EXPRESSION\",\"value\":{\"operator\":\"EQUALS\",\"valueType\":\"STRING\",\"value\":\"BR\"}}");
        var result = provider.get("platform.value", JsonNode.class, null);
        assertThat(result.get("operator").asString()).isEqualTo("EQUALS");
    }

    @Test
    void failuresAreControlledAndDoNotRetainSensitiveCauses() {
        for (String key : List.of("redis.unavailable", "redis.timeout", "redis.serialization", "redis.type")) {
            doThrow(new ApiException(key, new RuntimeException("SECRET_TOKEN_123")))
                    .when(store).get("platform.value", JsonNode.class);
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
