package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.operations.*;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

class RedisOperationsIT extends RedisIntegrationSupport {
    @Test
    void hashAndSetNativeCommandsAreBounded() {
        context()
                .run(
                        ctx -> {
                            var operations = ctx.getBean(RedisOperations.class);
                            String hash = key(), set = key();
                            operations.hashes().put(hash, "first", new Event("first", 1));
                            operations.hashes().put(hash, "second", new Event("second", 2));
                            assertThat(operations.hashes().get(hash, "first", Event.class))
                                    .contains(new Event("first", 1));
                            assertThat(operations.hashes().exists(hash, "second")).isTrue();
                            assertThatThrownBy(
                                            () -> operations.hashes().getAll(hash, Event.class, 1))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.limit");
                            assertThat(operations.hashes().getAll(hash, Event.class, 2)).hasSize(2);
                            assertThat(operations.hashes().remove(hash, "first")).isTrue();
                            assertThat(operations.sets().add(set, "one")).isTrue();
                            assertThat(operations.sets().add(set, "one")).isFalse();
                            operations.sets().add(set, "two");
                            assertThat(operations.sets().contains(set, "one")).isTrue();
                            assertThat(operations.sets().count(set)).isEqualTo(2);
                            assertThatThrownBy(
                                            () -> operations.sets().members(set, String.class, 1))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.limit");
                            assertThat(operations.sets().members(set, String.class, 2))
                                    .containsExactlyInAnyOrder("one", "two");
                            assertThat(operations.sets().remove(set, "one")).isTrue();
                        });
    }

    @Test
    void listAndSortedSetOrderingAndLimits() {
        context()
                .run(
                        ctx -> {
                            var operations = ctx.getBean(RedisOperations.class);
                            String list = key(), sorted = key();
                            operations.lists().pushRight(list, "middle");
                            operations.lists().pushLeft(list, "first");
                            operations.lists().pushRight(list, "last");
                            assertThat(operations.lists().range(list, 0, 2, String.class))
                                    .containsExactly("first", "middle", "last");
                            assertThat(operations.lists().length(list)).isEqualTo(3);
                            operations.lists().trim(list, 0, 1);
                            assertThat(operations.lists().popLeft(list, String.class))
                                    .contains("first");
                            assertThat(operations.lists().popRight(list, String.class))
                                    .contains("middle");
                            assertThat(operations.lists().popRight(list, String.class)).isEmpty();
                            operations.sortedSets().add(sorted, "high", 9);
                            operations.sortedSets().add(sorted, "low", 1);
                            assertThat(operations.sortedSets().range(sorted, 0, 1, String.class))
                                    .containsExactly("low", "high");
                            assertThat(operations.sortedSets().rank(sorted, "high")).contains(1L);
                            assertThat(operations.sortedSets().score(sorted, "low")).contains(1.0);
                            assertThat(operations.sortedSets().incrementScore(sorted, "low", 2))
                                    .isEqualTo(3.0);
                            assertThat(operations.sortedSets().count(sorted)).isEqualTo(2);
                            assertThat(operations.sortedSets().remove(sorted, "high")).isTrue();
                            assertThatThrownBy(
                                            () ->
                                                    operations
                                                            .lists()
                                                            .range(list, 0, -1, String.class))
                                    .isInstanceOf(ApiException.class);
                            assertThatThrownBy(
                                            () ->
                                                    operations
                                                            .sortedSets()
                                                            .range(sorted, 0, 1000, String.class))
                                    .isInstanceOf(ApiException.class);
                            assertThatThrownBy(
                                            () ->
                                                    operations
                                                            .sortedSets()
                                                            .add(sorted, "bad", Double.NaN))
                                    .isInstanceOf(ApiException.class);
                        });
    }

    @Test
    void compareAndSetIsAtomicAndPreservesExpiry() {
        context()
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            var atomic = ctx.getBean(RedisOperations.class).atomic();
                            String key = key();
                            assertThat(atomic.compareAndSet(key, null, "initial")).isTrue();
                            assertThat(atomic.compareAndSet(key, null, "other")).isFalse();
                            store.expire(key, java.time.Duration.ofSeconds(20));
                            try (var executor = Executors.newFixedThreadPool(8)) {
                                var futures = new ArrayList<Future<Boolean>>();
                                for (int i = 0; i < 20; i++)
                                    futures.add(
                                            executor.submit(
                                                    () ->
                                                            atomic.compareAndSet(
                                                                    key, "initial", "winner")));
                                int wins = 0;
                                for (var f : futures) if (f.get()) wins++;
                                assertThat(wins).isEqualTo(1);
                            }
                            assertThat(store.ttl(key)).isPresent();
                            assertThat(store.get(key, String.class)).contains("winner");
                        });
    }

    @Test
    void concurrentCountersAndWrongType() {
        context()
                .run(
                        ctx -> {
                            var operations = ctx.getBean(RedisOperations.class);
                            String key = key();
                            try (var executor = Executors.newFixedThreadPool(8)) {
                                var tasks = new ArrayList<Callable<Long>>();
                                for (int i = 0; i < 32; i++)
                                    tasks.add(() -> operations.atomic().increment(key, 1));
                                for (var result : executor.invokeAll(tasks)) result.get();
                            }
                            assertThat(operations.atomic().decrement(key, 2)).isEqualTo(30);
                            assertThatThrownBy(() -> operations.hashes().put(key, "field", "value"))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.type");
                        });
    }
}
