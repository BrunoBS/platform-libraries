package br.com.portalmanager.platform.library.redis.internal.operations;

import static br.com.portalmanager.platform.library.redis.internal.common.RedisErrors.call;

import br.com.portalmanager.platform.library.redis.internal.common.RedisErrors;
import br.com.portalmanager.platform.library.redis.operations.RedisKeys;
import java.util.function.Consumer;
import org.springframework.data.redis.core.ScanOptions;

final class DefaultRedisKeys implements RedisKeys {
    private final RedisOperationContext context;

    DefaultRedisKeys(RedisOperationContext context) { this.context = context; }

    public void scan(String prefix, int count, Consumer<String> consumer) {
        if (prefix == null || consumer == null) throw RedisErrors.invalid();
        context.access.limit(count);
        if (!prefix.isEmpty()) context.access.key(prefix);
        String physicalPrefix = context.access.prefix() + prefix;
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
                                context.redis.scan(
                                        ScanOptions.scanOptions()
                                                .match(pattern)
                                                .count(count)
                                                .build()));
        try {
            while (call(cursor::hasNext)) {
                String key = call(cursor::next);
                if (key.startsWith(physicalPrefix))
                    consumer.accept(key.substring(context.access.prefix().length()));
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
