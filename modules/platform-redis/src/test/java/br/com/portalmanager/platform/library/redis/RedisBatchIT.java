package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.core.AsyncRedisStore;
import br.com.portalmanager.platform.library.redis.core.RedisStore;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

class RedisBatchIT extends RedisIntegrationSupport {
    @Test
    void roundTripsOneThreeFiveHundredAndFiveHundredOneIndependentKeys() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var async = ctx.getBean(AsyncRedisStore.class);
                            for (int size : List.of(1, 3, 500, 501)) {
                                Map<String, String> values = new LinkedHashMap<>();
                                String prefix = key();
                                for (int i = 0; i < size; i++)
                                    values.put(prefix + ":" + i, "value-" + i);
                                store.setAll(values);
                                assertThat(store.getAll(values.keySet(), String.class))
                                        .containsExactlyEntriesOf(values);
                                assertThat(
                                                store.get(
                                                        values.keySet().iterator().next(),
                                                        String.class))
                                        .contains("value-0");
                                async.setAll(values).toCompletableFuture().join();
                                assertThat(
                                                async.getAll(values.keySet(), String.class)
                                                        .toCompletableFuture()
                                                        .join())
                                        .containsExactlyEntriesOf(values);
                            }
                        });
    }

    @Test
    void preservesAlignmentAndFirstOccurrenceOrderWithMissingKeysAndDuplicates() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var async = ctx.getBean(AsyncRedisStore.class);
                            String a = key(), b = key(), missing = key();
                            store.setAll(Map.of(a, "first", b, "last"));
                            var keys = List.of(missing, b, missing, a, b, key());
                            var expected = new LinkedHashMap<String, String>();
                            expected.put(b, "last");
                            expected.put(a, "first");
                            assertThat(store.getAll(keys, String.class))
                                    .containsExactlyEntriesOf(expected);
                            assertThat(
                                            async.getAll(keys, String.class)
                                                    .toCompletableFuture()
                                                    .join())
                                    .containsExactlyEntriesOf(expected);
                            assertThat(store.getAll(List.of(key(), key()), String.class)).isEmpty();
                        });
    }

    @Test
    void overwritesAtomicallyRemovesTtlAndUsesTheSameJsonCodecForObjectsGenericsAndLongValues() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var async = ctx.getBean(AsyncRedisStore.class);
                            var raw = ctx.getBean(StringRedisTemplate.class);
                            String a = key(), b = key();
                            store.set(a, "old", Duration.ofMinutes(5));
                            store.setAll(Map.of(a, "new", b, "á".repeat(10000)));
                            assertThat(raw.getExpire("redis-test:" + a, TimeUnit.MILLISECONDS))
                                    .isEqualTo(-1L);
                            assertThat(store.get(a, String.class)).contains("new");
                            assertThat(store.getAll(List.of(b), String.class).get(b))
                                    .hasSize(10000);
                            store.expire(a, Duration.ofMinutes(5));
                            async.setAll(Map.of(a, "async")).toCompletableFuture().join();
                            assertThat(raw.getExpire("redis-test:" + a, TimeUnit.MILLISECONDS))
                                    .isEqualTo(-1L);
                            Map<String, Event> objects =
                                    Map.of(a, new Event("one", 1), b, new Event("two", 2));
                            store.setAll(objects);
                            assertThat(store.getAll(objects.keySet(), Event.class))
                                    .containsAllEntriesOf(objects);
                            assertThat(
                                            async.getAll(objects.keySet(), Event.class)
                                                    .toCompletableFuture()
                                                    .join())
                                    .containsAllEntriesOf(objects);
                            Map<String, List<String>> lists =
                                    Map.of(a, List.of("ADMIN", "READER"), b, List.of("READER"));
                            var type = new TypeReference<List<String>>() {};
                            async.setAll(lists).toCompletableFuture().join();
                            assertThat(store.getAll(lists.keySet(), type))
                                    .containsAllEntriesOf(lists);
                            assertThat(
                                            async.getAll(lists.keySet(), type)
                                                    .toCompletableFuture()
                                                    .join())
                                    .containsAllEntriesOf(lists);
                        });
    }

    @Test
    void validatesAllEntriesBeforeWritingAndUsesExistingLimitsAndErrors() {
        context()
                .withPropertyValues(
                        "platform.redis.max-batch-size=500", "platform.redis.max-read-size=500")
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var async = ctx.getBean(AsyncRedisStore.class);
                            String valid = key();
                            Map<String, Object> invalid = new LinkedHashMap<>();
                            invalid.put(valid, "valid");
                            invalid.put("", "bad");
                            assertThatThrownBy(() -> store.setAll(invalid))
                                    .hasMessage("redis.validation");
                            assertThat(store.exists(valid)).isFalse();
                            invalid.remove("");
                            invalid.put("null-value", null);
                            assertThatThrownBy(() -> store.setAll(invalid))
                                    .hasMessage("redis.validation");
                            assertThatThrownBy(
                                            () ->
                                                    async.setAll(invalid)
                                                            .toCompletableFuture()
                                                            .join())
                                    .hasCauseInstanceOf(ApiException.class);
                            assertThat(store.exists(valid)).isFalse();
                            Map<String, Integer> many = new LinkedHashMap<>();
                            for (int i = 0; i < 501; i++) many.put(valid + i, i);
                            assertThatThrownBy(() -> store.setAll(many))
                                    .hasMessage("redis.validation");
                            assertThatThrownBy(() -> store.getAll(many.keySet(), Integer.class))
                                    .hasMessage("redis.validation");
                            assertThatThrownBy(
                                            () ->
                                                    store.getAll(
                                                            Arrays.asList(valid, null),
                                                            String.class))
                                    .hasMessage("redis.validation");
                            assertThatThrownBy(
                                            () ->
                                                    store.getAll(
                                                            List.of(valid), (Class<String>) null))
                                    .hasMessage("redis.validation");
                        });
    }

    @Test
    void malformedJsonTypeMismatchAndExternalNullAreErrorsRatherThanMissingEntries() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var async = ctx.getBean(AsyncRedisStore.class);
                            var raw = ctx.getBean(StringRedisTemplate.class);
                            String name = key();
                            for (String json : List.of("not-json", "null", "123")) {
                                raw.opsForValue().set("redis-test:" + name, json);
                                assertThatThrownBy(
                                                () ->
                                                        store.getAll(
                                                                List.of(key(), name), Event.class))
                                        .hasMessage("redis.serialization");
                                assertThatThrownBy(
                                                () ->
                                                        async.getAll(
                                                                        List.of(key(), name),
                                                                        Event.class)
                                                                .toCompletableFuture()
                                                                .join())
                                        .hasCauseInstanceOf(ApiException.class);
                            }
                        });
    }

    @Test
    void oversizedPayloadsAreRejectedAndNamespacesRemainIsolated() {
        context()
                .withPropertyValues("platform.redis.max-payload-bytes=32")
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var raw = ctx.getBean(StringRedisTemplate.class);
                            String name = key();
                            assertThatThrownBy(() -> store.setAll(Map.of(name, "x".repeat(33))))
                                    .hasMessage("redis.limit");
                            raw.opsForValue().set("foreign:" + name, "\"foreign\"");
                            assertThat(store.getAll(List.of(name), String.class)).isEmpty();
                            raw.opsForValue()
                                    .set("redis-test:" + name, "\"" + "x".repeat(33) + "\"");
                            assertThatThrownBy(() -> store.getAll(List.of(name), String.class))
                                    .hasMessage("redis.limit");
                        });
    }

    @Test
    void connectionFailuresAreNotConvertedToEmptyMapsAndEmptyBatchesDoNotConnect() {
        context()
                .withPropertyValues(
                        "spring.data.redis.port=1",
                        "spring.data.redis.timeout=100ms",
                        "spring.data.redis.connect-timeout=100ms")
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var async = ctx.getBean(AsyncRedisStore.class);
                            store.setAll(Map.of());
                            assertThat(store.getAll(List.of(), String.class)).isEmpty();
                            async.setAll(Map.of()).toCompletableFuture().join();
                            assertThat(
                                            async.getAll(List.of(), String.class)
                                                    .toCompletableFuture()
                                                    .join())
                                    .isEmpty();
                            assertThatThrownBy(() -> store.setAll(Map.of("one", "value")))
                                    .isInstanceOf(ApiException.class);
                            assertThatThrownBy(() -> store.getAll(List.of("one"), String.class))
                                    .isInstanceOf(ApiException.class);
                            assertThatThrownBy(
                                            () ->
                                                    async.setAll(Map.of("one", "value"))
                                                            .toCompletableFuture()
                                                            .join())
                                    .hasCauseInstanceOf(ApiException.class);
                            assertThatThrownBy(
                                            () ->
                                                    async.getAll(List.of("one"), String.class)
                                                            .toCompletableFuture()
                                                            .join())
                                    .hasCauseInstanceOf(ApiException.class);
                        });
    }
}
