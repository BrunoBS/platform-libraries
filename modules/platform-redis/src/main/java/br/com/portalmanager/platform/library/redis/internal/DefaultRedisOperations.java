package br.com.portalmanager.platform.library.redis.internal;

import static br.com.portalmanager.platform.library.redis.internal.RedisErrors.call;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.operations.RedisAtomic;
import br.com.portalmanager.platform.library.redis.operations.RedisHashes;
import br.com.portalmanager.platform.library.redis.operations.RedisKeys;
import br.com.portalmanager.platform.library.redis.operations.RedisLists;
import br.com.portalmanager.platform.library.redis.operations.RedisOperations;
import br.com.portalmanager.platform.library.redis.operations.RedisSets;
import br.com.portalmanager.platform.library.redis.operations.RedisSortedSets;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class DefaultRedisOperations implements RedisOperations {
    private static final DefaultRedisScript<List> HASH_READ =
            new DefaultRedisScript<>(
                    """
                    if redis.call('HLEN',KEYS[1]) > tonumber(ARGV[1]) then return redis.error_reply('PLATFORM_REDIS_LIMIT') end
                    return redis.call('HGETALL',KEYS[1])
                    """,
                    List.class);
    private static final DefaultRedisScript<List> SET_READ =
            new DefaultRedisScript<>(
                    """
                    if redis.call('SCARD',KEYS[1]) > tonumber(ARGV[1]) then return redis.error_reply('PLATFORM_REDIS_LIMIT') end
                    return redis.call('SMEMBERS',KEYS[1])
                    """,
                    List.class);
    private static final DefaultRedisScript<Long> CAS =
            new DefaultRedisScript<>(
                    """
                    local old = redis.call('GET',KEYS[1])
                    if (ARGV[1] == 'absent' and not old) or (ARGV[1] == 'present' and old == ARGV[2]) then
                        redis.call('SET',KEYS[1],ARGV[3],'KEEPTTL')
                        return 1
                    end
                    return 0
                    """,
                    Long.class);
    private static final DefaultRedisScript<Long> GUARDED_SET =
            new DefaultRedisScript<>(
                    "if redis.call('GET',KEYS[1]) ~= ARGV[1] then return 0 end "
                            + "redis.call('SET',KEYS[2],ARGV[2]); return 1",
                    Long.class);
    private static final DefaultRedisScript<Long> GUARDED_DELETE =
            new DefaultRedisScript<>(
                    "if redis.call('GET',KEYS[1]) ~= ARGV[1] then return 0 end "
                            + "redis.call('DEL',KEYS[2]); return 1",
                    Long.class);
    private static final DefaultRedisScript<Long> COMPARE_DELETE =
            new DefaultRedisScript<>(
                    "if redis.call('GET',KEYS[1]) == ARGV[1] then return redis.call('DEL',KEYS[1])"
                        + " end return 0",
                    Long.class);
    private final RedisKeys keys = new Keys();
    private final StringRedisTemplate redis;
    private final RedisJsonCodec codec;
    private final RedisAccess access;
    private final RedisStore store;
    private final RedisHashes hashes = new Hashes();
    private final RedisSets sets = new Sets();
    private final RedisLists lists = new Lists();
    private final RedisSortedSets sortedSets = new SortedSets();
    private final RedisAtomic atomic = new Atomic();

    public DefaultRedisOperations(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access, RedisStore store) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
        this.store = store;
    }

    public RedisKeys keys() {
        return keys;
    }

    public RedisHashes hashes() {
        return hashes;
    }

    public RedisSets sets() {
        return sets;
    }

    public RedisLists lists() {
        return lists;
    }

    public RedisSortedSets sortedSets() {
        return sortedSets;
    }

    public RedisAtomic atomic() {
        return atomic;
    }

    private void target(Class<?> type) {
        if (type == null) throw RedisErrors.invalid();
    }

    private void finite(double value) {
        if (!Double.isFinite(value)) throw RedisErrors.invalid();
    }

    private final class Keys implements RedisKeys {
        public void scan(String prefix, int count, Consumer<String> consumer) {
            if (prefix == null || consumer == null) throw RedisErrors.invalid();
            access.limit(count);
            if (!prefix.isEmpty()) access.key(prefix);
            String physicalPrefix = access.prefix() + prefix;
            String pattern =
                    physicalPrefix
                                    .replace("\\", "\\\\")
                                    .replace("*", "\\*")
                                    .replace("?", "\\?")
                                    .replace("[", "\\[")
                                    .replace("]", "\\]")
                            + "*";
            var cursor =
                    call(
                            () ->
                                    redis.scan(
                                            ScanOptions.scanOptions()
                                                    .match(pattern)
                                                    .count(count)
                                                    .build()));
            try {
                while (call(cursor::hasNext)) {
                    String key = call(cursor::next);
                    if (key.startsWith(physicalPrefix))
                        consumer.accept(key.substring(access.prefix().length()));
                }
            } finally {
                call(
                        () -> {
                            cursor.close();
                            return null;
                        });
            }
        }
    }

    private List<String> guardedKeys(String guardKey, String key) {
        String guard = access.key(guardKey);
        String target = access.key(key);
        if (guard.equals(target)) throw RedisErrors.invalid();
        return List.of(guard, target);
    }

    private final class Hashes implements RedisHashes {
        public <T> void put(String key, String field, T value) {
            call(
                    () -> {
                        redis.opsForHash()
                                .put(access.key(key), access.text(field), codec.encode(value));
                        return null;
                    });
        }

        public <T> Optional<T> get(String key, String field, Class<T> type) {
            target(type);
            Object json = call(() -> redis.opsForHash().get(access.key(key), access.text(field)));
            return json == null ? Optional.empty() : Optional.of(codec.decode((String) json, type));
        }

        public <T> Map<String, T> getAll(String key, Class<T> type, int limit) {
            target(type);
            access.limit(limit);
            List<?> entries =
                    call(
                            () ->
                                    redis.execute(
                                            HASH_READ,
                                            List.of(access.key(key)),
                                            Integer.toString(limit)));
            Map<String, T> values = new LinkedHashMap<>();
            for (int index = 0; index < entries.size(); index += 2)
                values.put(
                        (String) entries.get(index),
                        codec.decode((String) entries.get(index + 1), type));
            return Collections.unmodifiableMap(values);
        }

        public boolean remove(String key, String field) {
            return call(() -> redis.opsForHash().delete(access.key(key), access.text(field)) > 0);
        }

        public boolean exists(String key, String field) {
            return call(() -> redis.opsForHash().hasKey(access.key(key), access.text(field)));
        }
    }

    private final class Sets implements RedisSets {
        public <T> boolean add(String key, T value) {
            return call(() -> redis.opsForSet().add(access.key(key), codec.encode(value)) > 0);
        }

        public <T> boolean remove(String key, T value) {
            return call(() -> redis.opsForSet().remove(access.key(key), codec.encode(value)) > 0);
        }

        public <T> boolean contains(String key, T value) {
            return call(
                    () ->
                            Boolean.TRUE.equals(
                                    redis.opsForSet()
                                            .isMember(access.key(key), codec.encode(value))));
        }

        public <T> Set<T> members(String key, Class<T> type, int limit) {
            target(type);
            access.limit(limit);
            List<?> entries =
                    call(
                            () ->
                                    redis.execute(
                                            SET_READ,
                                            List.of(access.key(key)),
                                            Integer.toString(limit)));
            Set<T> values = new LinkedHashSet<>();
            for (Object entry : entries) values.add(codec.decode((String) entry, type));
            return Collections.unmodifiableSet(values);
        }

        public long count(String key) {
            return call(() -> redis.opsForSet().size(access.key(key)));
        }
    }

    private final class Lists implements RedisLists {
        public <T> long pushLeft(String key, T value) {
            return call(() -> redis.opsForList().leftPush(access.key(key), codec.encode(value)));
        }

        public <T> long pushRight(String key, T value) {
            return call(() -> redis.opsForList().rightPush(access.key(key), codec.encode(value)));
        }

        public <T> Optional<T> popLeft(String key, Class<T> type) {
            target(type);
            return Optional.ofNullable(call(() -> redis.opsForList().leftPop(access.key(key))))
                    .map(json -> codec.decode(json, type));
        }

        public <T> Optional<T> popRight(String key, Class<T> type) {
            target(type);
            return Optional.ofNullable(call(() -> redis.opsForList().rightPop(access.key(key))))
                    .map(json -> codec.decode(json, type));
        }

        public <T> List<T> range(String key, long start, long end, Class<T> type) {
            target(type);
            access.range(start, end);
            return call(() -> redis.opsForList().range(access.key(key), start, end)).stream()
                    .map(json -> codec.decode(json, type))
                    .toList();
        }

        public long length(String key) {
            return call(() -> redis.opsForList().size(access.key(key)));
        }

        public void trim(String key, long start, long end) {
            access.range(start, end);
            call(
                    () -> {
                        redis.opsForList().trim(access.key(key), start, end);
                        return null;
                    });
        }
    }

    private final class SortedSets implements RedisSortedSets {
        public <T> boolean add(String key, T value, double score) {
            finite(score);
            return call(
                    () ->
                            Boolean.TRUE.equals(
                                    redis.opsForZSet()
                                            .add(access.key(key), codec.encode(value), score)));
        }

        public <T> boolean remove(String key, T value) {
            return call(() -> redis.opsForZSet().remove(access.key(key), codec.encode(value)) > 0);
        }

        public <T> Optional<Double> score(String key, T value) {
            return Optional.ofNullable(
                    call(() -> redis.opsForZSet().score(access.key(key), codec.encode(value))));
        }

        public <T> double incrementScore(String key, T value, double delta) {
            finite(delta);
            return call(
                    () ->
                            redis.opsForZSet()
                                    .incrementScore(access.key(key), codec.encode(value), delta));
        }

        public <T> Set<T> range(String key, long start, long end, Class<T> type) {
            target(type);
            access.range(start, end);
            Set<T> values = new LinkedHashSet<>();
            for (String json : call(() -> redis.opsForZSet().range(access.key(key), start, end)))
                values.add(codec.decode(json, type));
            return Collections.unmodifiableSet(values);
        }

        public <T> Optional<Long> rank(String key, T value) {
            return Optional.ofNullable(
                    call(() -> redis.opsForZSet().rank(access.key(key), codec.encode(value))));
        }

        public long count(String key) {
            return call(() -> redis.opsForZSet().size(access.key(key)));
        }
    }

    private final class Atomic implements RedisAtomic {
        public <T> boolean setIfValue(String guardKey, Object expectedGuard, String key, T value) {
            var keys = guardedKeys(guardKey, key);
            String expected = codec.encode(expectedGuard);
            String json = codec.encode(value);
            return Long.valueOf(1)
                    .equals(call(() -> redis.execute(GUARDED_SET, keys, expected, json)));
        }

        public boolean deleteIfValue(String guardKey, Object expectedGuard, String key) {
            var keys = guardedKeys(guardKey, key);
            String expected = codec.encode(expectedGuard);
            return Long.valueOf(1)
                    .equals(call(() -> redis.execute(GUARDED_DELETE, keys, expected)));
        }

        public boolean compareAndDelete(String key, Object expected) {
            String physical = access.key(key);
            String json = codec.encode(expected);
            return Long.valueOf(1)
                    .equals(call(() -> redis.execute(COMPARE_DELETE, List.of(physical), json)));
        }

        public long increment(String key, long delta) {
            if (delta < 1) throw RedisErrors.invalid();
            return call(() -> redis.opsForValue().increment(access.key(key), delta));
        }

        public long decrement(String key, long delta) {
            if (delta < 1) throw RedisErrors.invalid();
            return call(() -> redis.opsForValue().decrement(access.key(key), delta));
        }

        public <T> boolean setIfAbsent(String key, T value) {
            return store.setIfAbsent(key, value);
        }

        public <T> boolean compareAndSet(String key, T expected, T replacement) {
            String expectedJson = expected == null ? "" : codec.encode(expected);
            String json = codec.encode(replacement);
            return call(
                    () ->
                            Long.valueOf(1)
                                    .equals(
                                            redis.execute(
                                                    CAS,
                                                    List.of(access.key(key)),
                                                    expected == null ? "absent" : "present",
                                                    expectedJson,
                                                    json)));
        }
    }
}
