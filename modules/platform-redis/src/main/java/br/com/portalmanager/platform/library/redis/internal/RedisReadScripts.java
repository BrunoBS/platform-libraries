package br.com.portalmanager.platform.library.redis.internal;

import org.springframework.data.redis.core.script.DefaultRedisScript;

/** Rejects oversized external String values before sending them to the client. */
public final class RedisReadScripts {
    private RedisReadScripts() {}

    public static final DefaultRedisScript<String> GET =
            new DefaultRedisScript<>(
                    """
                    if redis.call('STRLEN',KEYS[1]) > tonumber(ARGV[1]) then return redis.error_reply('PLATFORM_REDIS_LIMIT') end
                    return redis.call('GET',KEYS[1])
                    """,
                    String.class);
}
