package br.com.portalmanager.core.messaging.cache;

import br.com.portalmanager.core.messaging.model.ApiMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

public class RedisApiMessageCache implements ApiMessageCache {

    private static final String DELIMITER = "\u0000";
    private static final String CACHE_PREFIX = "platform:message:";

    private static final int INDEX_CODE = 0;
    private static final int INDEX_MESSAGE_KEY = 1;
    private static final int INDEX_LOCALE = 2;
    private static final int INDEX_MESSAGE = 3;
    private static final int INDEX_SOLUTION = 4;
    private static final int INDEX_HTTP_STATUS = 5;

    private static final int TOTAL_EXPECTED_FIELDS = 6;

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public RedisApiMessageCache(StringRedisTemplate redisTemplate, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
    }

    private String key(String k, Locale l) {
        return CACHE_PREFIX + k + ":" + l.toLanguageTag();
    }

    @Override
    public Optional<ApiMessage> get(String k, Locale l) {
        try {
            String v = redisTemplate.opsForValue().get(key(k, l));
            if (v == null || v.isBlank()) {
                return Optional.empty();
            }

            String[] p = v.split(DELIMITER, -1);
            if (p.length != TOTAL_EXPECTED_FIELDS) {
                return Optional.empty();
            }

            return Optional.of(new ApiMessage(
                    p[INDEX_CODE],
                    p[INDEX_MESSAGE_KEY],
                    p[INDEX_LOCALE],
                    p[INDEX_MESSAGE],
                    p[INDEX_SOLUTION],
                    Integer.parseInt(p[INDEX_HTTP_STATUS])
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(ApiMessage m) {
        try {
            String v = String.join(DELIMITER,
                    m.code(),
                    m.messageKey(),
                    m.locale(),
                    m.message(),
                    m.solution() == null ? "" : m.solution(),
                    String.valueOf(m.httpStatus())
            );

            redisTemplate.opsForValue().set(key(m.messageKey(), Locale.forLanguageTag(m.locale())), v, ttl);
        } catch (Exception ignored) {
        }
    }

    public void evict(String k, Locale l) {
        try {
            redisTemplate.delete(key(k, l));
        } catch (Exception ignored) {
        }
    }
}
