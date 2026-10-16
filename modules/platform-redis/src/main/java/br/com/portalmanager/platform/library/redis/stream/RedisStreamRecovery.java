package br.com.portalmanager.platform.library.redis.stream;

import java.util.List;

/**
 * One bounded pending window. nextId=0-0 wraps to the beginning; empty records may still advance.
 */
public record RedisStreamRecovery<T>(String nextId, List<RedisStreamRecord<T>> records) {
    public RedisStreamRecovery {
        records = List.copyOf(records);
    }
}
