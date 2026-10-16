package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import br.com.portalmanager.platform.library.redis.stream.GroupStart;
import br.com.portalmanager.platform.library.redis.stream.RedisStreamRecord;
import br.com.portalmanager.platform.library.redis.stream.RedisStreamRecovery;
import br.com.portalmanager.platform.library.redis.stream.RedisStreams;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class DefaultRedisStreams implements RedisStreams {
    private final StringRedisTemplate redis;
    private final RedisJsonCodec codec;
    private final RedisAccess access;

    public DefaultRedisStreams(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
    }

    private StreamOperations<String, String, String> operations() {
        return redis.opsForStream();
    }

    public <T> String append(String stream, T payload) {
        return append(stream, payload, access.properties().streamMaxLength());
    }

    public <T> String append(String stream, T payload, long maxLength) {
        retention(maxLength);
        String key = access.key(stream);
        String json = codec.encode(payload);
        return RedisErrors.call(
                () ->
                        operations()
                                .add(
                                        StreamRecords.string(Map.of("payload", json))
                                                .withStreamKey(key),
                                        XAddOptions.maxlen(maxLength))
                                .getValue());
    }

    public <T> List<RedisStreamRecord<T>> read(
            String stream, String afterId, int count, Duration block, Class<T> type) {
        target(type);
        id(afterId);
        return decode(
                RedisErrors.call(
                        () ->
                                operations()
                                        .read(
                                                options(count, block),
                                                StreamOffset.create(
                                                        access.key(stream),
                                                        ReadOffset.from(afterId)))),
                type);
    }

    public void createGroup(String stream, String group, GroupStart start) {
        String key = access.key(stream);
        access.text(group);
        if (start == null) throw RedisErrors.invalid();
        try {
            redis.execute(
                    (RedisCallback<String>)
                            connection ->
                                    connection
                                            .streamCommands()
                                            .xGroupCreate(
                                                    key.getBytes(StandardCharsets.UTF_8),
                                                    group,
                                                    start == GroupStart.BEGINNING
                                                            ? ReadOffset.from("0-0")
                                                            : ReadOffset.latest(),
                                                    true));
        } catch (RuntimeException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause())
                if (cause.getMessage() != null && cause.getMessage().startsWith("BUSYGROUP"))
                    return;
            throw RedisErrors.translate(failure);
        }
    }

    public <T> List<RedisStreamRecord<T>> readGroup(
            String stream,
            String group,
            String consumer,
            int count,
            Duration block,
            Class<T> type) {
        target(type);
        return decode(readGroupRaw(stream, group, consumer, count, block), type);
    }

    List<MapRecord<String, String, String>> readGroupRaw(
            String stream, String group, String consumer, int count, Duration block) {
        return RedisErrors.call(
                () ->
                        operations()
                                .read(
                                        Consumer.from(access.text(group), access.text(consumer)),
                                        options(count, block),
                                        StreamOffset.create(
                                                access.key(stream), ReadOffset.lastConsumed())));
    }

    public long acknowledge(String stream, String group, List<String> ids) {
        if (ids == null) throw RedisErrors.invalid();
        access.batch(ids.size());
        for (String id : ids) id(id);
        return RedisErrors.call(
                () ->
                        operations()
                                .acknowledge(
                                        access.key(stream),
                                        access.text(group),
                                        ids.toArray(String[]::new)));
    }

    public <T> List<RedisStreamRecord<T>> recover(
            String stream,
            String group,
            String consumer,
            Duration minIdle,
            int count,
            Class<T> type) {
        return recover(stream, group, consumer, "0-0", minIdle, count, type).records();
    }

    public <T> RedisStreamRecovery<T> recover(
            String stream,
            String group,
            String consumer,
            String afterId,
            Duration minIdle,
            int count,
            Class<T> type) {
        target(type);
        var window = recoverRaw(stream, group, consumer, afterId, minIdle, count);
        return new RedisStreamRecovery<>(window.nextId(), decode(window.records(), type));
    }

    record RecoveryWindow(String nextId, List<MapRecord<String, String, String>> records) {}

    RecoveryWindow recoverRaw(
            String stream,
            String group,
            String consumer,
            String afterId,
            Duration minIdle,
            int count) {
        String key = access.key(stream);
        access.text(group);
        access.text(consumer);
        access.duration(minIdle);
        access.limit(count);
        access.batch(count);
        id(afterId);
        // Lettuce XPendingArgs emits boundary values without their exclusive flag.
        // Redis 6.2+ accepts the literal '(' prefix for an exclusive stream ID.
        Range<String> range =
                Range.of(Range.Bound.inclusive("(" + afterId), Range.Bound.unbounded());
        return RedisErrors.call(
                () -> {
                    var pending = operations().pending(key, group, range, count);
                    if (pending.isEmpty()) return new RecoveryWindow("0-0", List.of());
                    String next =
                            pending.size() < count
                                    ? "0-0"
                                    : pending.get(pending.size() - 1).getIdAsString();
                    RecordId[] eligible =
                            pending.stream()
                                    .filter(
                                            item ->
                                                    item.getElapsedTimeSinceLastDelivery()
                                                                    .compareTo(minIdle)
                                                            >= 0)
                                    .map(PendingMessage::getId)
                                    .toArray(RecordId[]::new);
                    if (eligible.length == 0) return new RecoveryWindow(next, List.of());
                    return new RecoveryWindow(
                            next, operations().claim(key, group, consumer, minIdle, eligible));
                });
    }

    public long trim(String stream, long maxLength) {
        retention(maxLength);
        return RedisErrors.call(() -> operations().trim(access.key(stream), maxLength, false));
    }

    void retention(long length) {
        if (length < 1 || length > access.properties().streamMaxLength())
            throw RedisErrors.invalid();
    }

    private StreamReadOptions options(int count, Duration block) {
        access.limit(count);
        access.batch(count);
        if (block == null || block.isNegative() || block.compareTo(Duration.ofSeconds(5)) > 0)
            throw RedisErrors.invalid();
        return block.isZero()
                ? StreamReadOptions.empty().count(count)
                : StreamReadOptions.empty().count(count).block(access.duration(block));
    }

    private void id(String id) {
        if (id == null || !id.matches("[0-9]+-[0-9]+") || id.length() > 41)
            throw RedisErrors.invalid();
    }

    private void target(Class<?> type) {
        if (type == null) throw RedisErrors.invalid();
    }

    private <T> List<RedisStreamRecord<T>> decode(
            List<MapRecord<String, String, String>> records, Class<T> type) {
        if (records == null) return List.of();
        return records.stream()
                .map(
                        record ->
                                new RedisStreamRecord<T>(
                                        record.getId().getValue(),
                                        codec.decode(record.getValue().get("payload"), type)))
                .toList();
    }
}
