package br.com.portalmanager.platform.library.messaging.internal.cache;

import br.com.portalmanager.platform.library.messaging.api.model.ApiMessage;
import br.com.portalmanager.platform.library.redis.api.store.RedisStore;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public class RedisApiMessageCache implements ApiMessageCache {

    private static final String CACHE_PREFIX = "platform:message:";

    private final RedisStore redisStore;
    private final Duration ttl;

    public RedisApiMessageCache(RedisStore redisStore, Duration ttl) {
        this.redisStore = redisStore;
        this.ttl = ttl;
    }

    private String key(String k, Locale l) {
        return CACHE_PREFIX + k + ":" + l.toLanguageTag();
    }

    @Override
    public Optional<ApiMessage> get(String k, Locale l) {
        try {
            return redisStore.get(key(k, l), ApiMessage.class);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void put(ApiMessage m) {
        try {
            redisStore.set(key(m.messageKey(), Locale.forLanguageTag(m.locale())), m, ttl);
        } catch (RuntimeException ignored) {
            // Cache is fail-open when Redis is unavailable.
        }
    }

    public void evict(String k, Locale l) {
        try {
            redisStore.delete(key(k, l));
        } catch (RuntimeException ignored) {
            // Cache is fail-open when Redis is unavailable.
        }
    }
}
