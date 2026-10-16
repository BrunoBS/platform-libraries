package br.com.portalmanager.platform.library.redis.stream;

import java.time.Duration;
import java.util.List;

/** Retained streams with explicit consumer group and acknowledgements; at-least-once processing. */
public interface RedisStreams {
    <T> String append(String stream, T payload);

    <T> String append(String stream, T payload, long maxLength);

    <T> List<RedisStreamRecord<T>> read(
            String stream, String afterId, int count, Duration block, Class<T> type);

    void createGroup(String stream, String group, GroupStart start);

    <T> List<RedisStreamRecord<T>> readGroup(
            String stream, String group, String consumer, int count, Duration block, Class<T> type);

    long acknowledge(String stream, String group, List<String> ids);

    <T> List<RedisStreamRecord<T>> recover(
            String stream,
            String group,
            String consumer,
            Duration minIdle,
            int count,
            Class<T> type);

    <T> RedisStreamRecovery<T> recover(
            String stream,
            String group,
            String consumer,
            String afterId,
            Duration minIdle,
            int count,
            Class<T> type);

    long trim(String stream, long maxLength);
}
