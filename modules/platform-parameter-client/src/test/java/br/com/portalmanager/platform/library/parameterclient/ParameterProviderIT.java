package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import br.com.portalmanager.platform.library.parameterclient.configuration.ParameterClientAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.library.testing.redis.RedisTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Producer-compatible envelope fixtures; no dependency on the producer or its database/API. */
@WithRedis
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RedisTestConfiguration.class)
class ParameterProviderIT {
    static GenericContainer<?> redisContainer;
    static final JsonMapper JSON = JsonMapper.builder().build();

    @BeforeAll
    static void fixture(@Autowired GenericContainer<?> container) { redisContainer = container; }

    ApplicationContextRunner context() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                DataRedisAutoConfiguration.class, DataRedisReactiveAutoConfiguration.class,
                PlatformRedisAutoConfiguration.class, ParameterClientAutoConfiguration.class))
                .withPropertyValues("spring.application.name=parameter-consumer",
                        "spring.data.redis.host=" + redisContainer.getHost(),
                        "spring.data.redis.port=" + redisContainer.getMappedPort(6379),
                        "spring.data.redis.timeout=1s", "spring.data.redis.connect-timeout=1s",
                        "platform.redis.namespace-mode=SHARED", "platform.redis.namespace=dynamic-parameter");
    }

    @Test
    void interoperatesWithActualProducerShapeAndPrefixForEveryValueKind() {
        context().run(ctx -> {
            assertThat(ctx).hasSingleBean(ParameterProvider.class).hasSingleBean(RedisStore.class);
            var store = ctx.getBean(RedisStore.class);
            var provider = ctx.getBean(ParameterProvider.class);
            var raw = ctx.getBean(StringRedisTemplate.class);
            Map<String, Object> values = Map.of("orders.days", 30, "platform.audit-enabled", true,
                    "platform.mode", "STANDARD", "platform.settings", new Settings(2),
                    "platform.regions", List.of("BR", "US"));
            // Same setAll and PublishedValue field names as ParameterKeyPublisherService.send.
            var published = new java.util.LinkedHashMap<String, PublishedValue>();
            values.forEach((key, value) -> published.put(key, new PublishedValue("CONFIGURATION", JSON.valueToTree(value))));
            store.setAll(published);
            assertThat(provider.get("orders.days", 1)).isEqualTo(30);
            assertThat(provider.get("platform.audit-enabled", false)).isTrue();
            assertThat(provider.get("platform.mode", "fallback")).isEqualTo("STANDARD");
            assertThat(provider.get("platform.settings", Settings.class, null)).isEqualTo(new Settings(2));
            assertThat(provider.get("platform.regions", new TypeReference<List<String>>() {}, List.of()))
                    .containsExactly("BR", "US");
            values.forEach((key, value) -> {
                assertThat(raw.hasKey("dynamic-parameter:" + key)).isTrue();
                assertThat(raw.hasKey("dynamic-parameter:dynamic-parameter:" + key)).isFalse();
                assertThat(raw.hasKey("config:" + key)).isFalse();
                assertThat(store.ttl(key)).isEmpty();
                JsonNode envelope = JSON.readTree(raw.opsForValue().get("dynamic-parameter:" + key));
                assertThat(envelope.get("type").asString()).isEqualTo("CONFIGURATION");
                assertThat(envelope.get("value")).isEqualTo(JSON.valueToTree(value));
            });
            // Overwrite via the public store as specified in the implementation contract.
            store.set("orders.days", new PublishedValue("CONFIGURATION", JSON.valueToTree(60)));
            assertThat(provider.get("orders.days", 1)).isEqualTo(60);
            store.set("platform.audit-enabled", new PublishedValue("CONFIGURATION", JSON.valueToTree(false)));
            store.set("platform.mode", new PublishedValue("CONFIGURATION", JSON.valueToTree("UPDATED")));
            store.set("platform.settings", new PublishedValue("CONFIGURATION", JSON.valueToTree(new Settings(4))));
            store.set("platform.regions", new PublishedValue("CONFIGURATION", JSON.valueToTree(List.of("EU"))));
            assertThat(provider.get("platform.audit-enabled", true)).isFalse();
            assertThat(provider.get("platform.mode", "fallback")).isEqualTo("UPDATED");
            assertThat(provider.get("platform.settings", Settings.class, null)).isEqualTo(new Settings(4));
            assertThat(provider.get("platform.regions", new TypeReference<List<String>>() {}, List.of()))
                    .containsExactly("EU");
            assertThat(provider.get("other.days", 7)).isEqualTo(7);
            for (String key : values.keySet()) assertThat(store.delete(key)).isTrue();
            assertThat(provider.get("orders.days", 7)).isEqualTo(7);
            assertThat(provider.get("platform.audit-enabled", true)).isTrue();
            assertThat(provider.get("platform.mode", "fallback")).isEqualTo("fallback");
            var settings = new Settings(9);
            assertThat(provider.get("platform.settings", Settings.class, settings)).isSameAs(settings);
            assertThat(provider.get("platform.regions", new TypeReference<List<String>>() {}, List.of("fallback")))
                    .containsExactly("fallback");
        });
    }

    @Test
    void corruptNullAndCoercedValuesNeverReturnDefaults() {
        context().run(ctx -> {
            var raw = ctx.getBean(StringRedisTemplate.class);
            var provider = ctx.getBean(ParameterProvider.class);
            for (String json : List.of("not-json", "null", "{}",
                    "{\"type\":\"CONFIGURATION\",\"value\":null}",
                    "{\"type\":\"CONFIGURATION\",\"value\":\"30\"}")) {
                raw.opsForValue().set("dynamic-parameter:platform.bad", json);
                assertThatThrownBy(() -> provider.get("platform.bad", 99))
                        .hasMessage("parameter-client.value.invalid");
            }
            raw.delete("dynamic-parameter:platform.bad");
            raw.opsForList().leftPush("dynamic-parameter:platform.bad", "native-list");
            assertThatThrownBy(() -> provider.get("platform.bad", 99))
                    .hasMessage("parameter-client.value.invalid");
        });
    }

    @Test
    void ttlExpiryIsAbsenceButProducerOverwritesArePersistent() {
        context().run(ctx -> {
            var store = ctx.getBean(RedisStore.class);
            var provider = ctx.getBean(ParameterProvider.class);
            var envelope = new PublishedValue("CONFIGURATION", JSON.valueToTree(3));
            store.set("orders.ttl", envelope, Duration.ofMillis(100));
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(
                    () -> assertThat(provider.get("orders.ttl", 7)).isEqualTo(7));
            store.set("orders.ttl", envelope, Duration.ofMinutes(1));
            store.set("orders.ttl", new PublishedValue("CONFIGURATION", JSON.valueToTree(4)));
            assertThat(store.ttl("orders.ttl")).isEmpty();
            assertThat(provider.get("orders.ttl", 7)).isEqualTo(4);
        });
    }

    @Test
    void actualConnectionFailureAndTimeoutAreNotAbsence() {
        context().withPropertyValues("spring.data.redis.port=1",
                "spring.data.redis.timeout=100ms", "spring.data.redis.connect-timeout=100ms")
                .run(ctx -> assertThatThrownBy(() -> ctx.getBean(ParameterProvider.class)
                        .get("platform.missing", 7)).hasMessage("parameter-client.read.failed"));
        context().withPropertyValues("spring.data.redis.timeout=100ms").run(ctx -> {
            var provider = ctx.getBean(ParameterProvider.class);
            assertThat(provider.get("platform.missing", 7)).isEqualTo(7); // Warm connection first.
            ctx.getBean(StringRedisTemplate.class).execute((RedisCallback<Object>) connection ->
                    connection.execute("CLIENT", "PAUSE".getBytes(StandardCharsets.UTF_8),
                            "450".getBytes(StandardCharsets.UTF_8), "ALL".getBytes(StandardCharsets.UTF_8)));
            assertThatThrownBy(() -> provider.get("platform.missing", 7)).hasMessage("parameter-client.timeout");
        });
    }

    @Test
    void explicitDisableReusesRedisWithoutCreatingProvider() {
        context().withPropertyValues("platform.parameter-client.enabled=false")
                .run(ctx -> assertThat(ctx).hasSingleBean(RedisStore.class).doesNotHaveBean(ParameterProvider.class));
        context().withPropertyValues("platform.redis.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RedisStore.class).doesNotHaveBean(ParameterProvider.class));
    }

    /** Fixture serialized by the actual Workspace PublishedValue record and schema-validated.
     * Producer source is identical at e87aefd07d11175e11e9fc8ca99752d60bd75bc5 and 883ea071.
     */
    @Test
    void evaluatesActualProducerFixturesWithoutPrefixOrPrecisionLoss() throws Exception {
        var exact = JsonMapper.builder().enable(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
        JsonNode fixtures;
        try (var source = getClass().getResourceAsStream("/producer-published-values.json")) {
            fixtures = exact.readTree(source);
        }
        context().run(ctx -> {
            var store = ctx.getBean(RedisStore.class);
            var provider = ctx.getBean(ParameterProvider.class);
            var raw = ctx.getBean(StringRedisTemplate.class);
            store.setAll(Map.of("rules.minimum-age", fixtures.get(0), "rules.allowed-segments", fixtures.get(1),
                    "rules.long-limit", fixtures.get(2), "rules.decimal-limit", fixtures.get(3),
                    "platform.producer-settings", fixtures.get(4)));
            assertThat(provider.matches("rules.minimum-age", 21)).isTrue();
            assertThat(provider.matches("rules.minimum-age", 17, true)).isFalse();
            assertThat(provider.matches("rules.allowed-segments", "RETAIL", false)).isTrue();
            assertThat(provider.matches("rules.allowed-segments", "OTHER")).isFalse();
            assertThat(provider.matches("rules.long-limit", Long.MAX_VALUE)).isTrue();
            assertThat(provider.matches("rules.long-limit", Long.MAX_VALUE - 1)).isFalse();
            assertThat(provider.matches("rules.decimal-limit", new java.math.BigDecimal("123456789012345678901234567890.13"))).isTrue();
            assertThat(provider.matches("rules.decimal-limit", new java.math.BigDecimal("123456789012345678901234567890.12"))).isFalse();
            assertThat(provider.get("platform.producer-settings", ParameterProviderTest.MySettings.class, null))
                    .isEqualTo(new ParameterProviderTest.MySettings(5));
            assertThat(raw.opsForValue().get("dynamic-parameter:rules.minimum-age"))
                    .contains("EXPRESSION", "GREATER_THAN_OR_EQUALS");
            assertThat(raw.hasKey("config:rules.minimum-age")).isFalse();
            assertThat(raw.hasKey("dynamic-parameter:dynamic-parameter:rules.minimum-age")).isFalse();
            assertThat(store.ttl("rules.minimum-age")).isEmpty();
            store.set("rules.minimum-age", fixtures.get(2));
            assertThat(provider.matches("rules.minimum-age", 21L)).isFalse();
            assertThat(store.delete("rules.minimum-age")).isTrue();
            assertThatThrownBy(() -> provider.matches("rules.minimum-age", 21)).hasMessage("parameter-client.missing");
            assertThat(provider.matches("rules.minimum-age", 21, true)).isTrue();
            store.set("rules.expression-ttl", fixtures.get(0), Duration.ofMillis(100));
            assertThat(provider.matches("rules.expression-ttl", 21)).isTrue();
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    assertThatThrownBy(() -> provider.matches("rules.expression-ttl", 21)).hasMessage("parameter-client.missing"));
        });
    }

    @Test
    void expressionFailuresWithRealRedisHaveExplicitFallbackOnly() {
        context().run(ctx -> {
            var raw = ctx.getBean(StringRedisTemplate.class);
            var provider = ctx.getBean(ParameterProvider.class);
            for (String json : List.of("broken-json", "{}", "null",
                    "{\"type\":\"EXPRESSION\",\"value\":{\"operator\":\"AND\",\"valueType\":\"INTEGER\",\"value\":18}}")) {
                raw.opsForValue().set("dynamic-parameter:rules.corrupt", json);
                assertThatThrownBy(() -> provider.matches("rules.corrupt", 21)).isInstanceOf(
                        br.com.portalmanager.platform.library.messaging.exception.ApiException.class);
                assertThat(provider.matches("rules.corrupt", 21, true)).isTrue();
                assertThat(provider.matches("rules.corrupt", 21, false)).isFalse();
            }
        });
        context().withPropertyValues("spring.data.redis.port=1",
                "spring.data.redis.timeout=100ms", "spring.data.redis.connect-timeout=100ms").run(ctx -> {
            var provider = ctx.getBean(ParameterProvider.class);
            assertThatThrownBy(() -> provider.matches("rules.missing", 21)).hasMessage("parameter-client.read.failed");
            assertThat(provider.matches("rules.missing", 21, true)).isTrue();
            assertThat(provider.matches("rules.missing", 21, false)).isFalse();
        });
    }

    public record PublishedValue(String type, JsonNode value) {}
    public record Settings(int retries) {}
}
