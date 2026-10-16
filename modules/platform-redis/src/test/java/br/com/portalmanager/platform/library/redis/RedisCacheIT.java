package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.cache.*;
import br.com.portalmanager.platform.library.redis.internal.RedisAccess;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.*;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class RedisCacheIT extends RedisIntegrationSupport {
    record Message(String text, int version) {}

    public static class Queries {
        final AtomicInteger reads = new AtomicInteger();

        public int readCount() {
            return reads.get();
        }

        @Cacheable(cacheNames = "messages", key = "#id")
        public Message find(String id) {
            return new Message(id, reads.incrementAndGet());
        }

        @Cacheable(cacheNames = "unknown", key = "#id")
        public Message unknown(String id) {
            return new Message(id, reads.incrementAndGet());
        }

        @CachePut(cacheNames = "messages", key = "#id")
        public Message update(String id) {
            return new Message(id, reads.incrementAndGet());
        }

        @CacheEvict(cacheNames = "messages", key = "#id")
        public void evict(String id) {}

        @Caching(
                evict = {
                    @CacheEvict(cacheNames = "messages", key = "#id"),
                    @CacheEvict(cacheNames = "unknown", key = "#id")
                })
        public void both(String id) {}

        @CacheEvict(cacheNames = "messages", allEntries = true)
        public void clear() {}

        @Cacheable(cacheNames = "list", key = "#id")
        public List<Message> list(String id) {
            reads.incrementAndGet();
            return List.of(new Message(id, 1));
        }

        @Cacheable(cacheNames = "map", key = "#id")
        public Map<String, List<Message>> map(String id) {
            reads.incrementAndGet();
            return Map.of(id, List.of(new Message(id, 1)));
        }

        @Cacheable(cacheNames = "optional", key = "#id")
        public Optional<Message> optional(String id) {
            reads.incrementAndGet();
            return Optional.of(new Message(id, 1));
        }

        @Cacheable(cacheNames = "nullable", key = "#id")
        public Message nullable(String id) {
            reads.incrementAndGet();
            return null;
        }

        @Cacheable(cacheNames = "syncnull", key = "#id", sync = true)
        public Message syncnull(String id) {
            reads.incrementAndGet();
            return null;
        }

        @Cacheable(cacheNames = "failure", key = "#id")
        public Message failure(String id) {
            reads.incrementAndGet();
            throw new UnsupportedOperationException("repository failure");
        }

        @Cacheable(cacheNames = "secure", key = "#tenant+':'+#user+':'+#locale+':'+#id")
        public Message secure(String tenant, String user, String locale, String id) {
            reads.incrementAndGet();
            return new Message(tenant + user + locale + id, 1);
        }

        @Cacheable(cacheNames = "sync", key = "#id", sync = true)
        public Message sync(String id) {
            int count = reads.incrementAndGet();
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new Message(id, count);
        }

        @Cacheable(cacheNames = "named", key = "#id", cacheManager = "platformRedisCacheManager")
        public Message named(String id) {
            return new Message(id, reads.incrementAndGet());
        }
    }

    ApplicationContextRunner caching() {
        return context()
                .withConfiguration(AutoConfigurations.of(PlatformRedisCacheAutoConfiguration.class))
                .withPropertyValues("platform.redis.cache.enabled=true")
                .withBean(Queries.class, Queries::new);
    }

    String physical(org.springframework.context.ApplicationContext ctx, String region, String key) {
        var cache = ctx.getBean(CacheManager.class).getCache(region);
        if (cache
                instanceof org.springframework.cache.transaction.TransactionAwareCacheDecorator tx)
            cache = tx.getTargetCache();
        String prefix =
                ((org.springframework.data.redis.cache.RedisCache) cache)
                        .getCacheConfiguration()
                        .getKeyPrefixFor(region);
        assertThat(prefix)
                .isEqualTo(
                        ctx.getBean(RedisAccess.class).key("cache:redis-test:default:" + region)
                                + "::");
        return prefix + key;
    }

    @Test
    void annotationsAreOffByDefaultAndCustomManagersHavePrecedence() {
        context()
                .withConfiguration(AutoConfigurations.of(PlatformRedisCacheAutoConfiguration.class))
                .withBean(Queries.class, Queries::new)
                .run(
                        ctx -> {
                            assertThat(ctx).doesNotHaveBean(CacheManager.class);
                            var q = ctx.getBean(Queries.class);
                            q.find("off");
                            q.find("off");
                            assertThat(q.readCount()).isEqualTo(2);
                        });
        var custom = new ConcurrentMapCacheManager();
        caching()
                .withBean(CacheManager.class, () -> custom)
                .run(
                        ctx -> {
                            assertThat(ctx.getBean(CacheManager.class)).isSameAs(custom);
                            assertThat(ctx).doesNotHaveBean(PlatformRedisCacheManager.class);
                            var q = ctx.getBean(Queries.class);
                            q.find("custom");
                            q.find("custom");
                            assertThat(q.readCount()).isEqualTo(1);
                        });
    }

    @Test
    void dynamicallyResolvesRegionsHitMissPutEvictCachingAndClear() {
        caching()
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            String id = key();
                            assertThat(q.find(id)).isEqualTo(q.find(id));
                            assertThat(q.readCount()).isEqualTo(1);
                            assertThat(q.update(id)).isEqualTo(q.find(id));
                            assertThat(q.readCount()).isEqualTo(2);
                            q.evict(id);
                            q.find(id);
                            assertThat(q.readCount()).isEqualTo(3);
                            q.unknown(id);
                            q.both(id);
                            q.find(id);
                            q.unknown(id);
                            assertThat(q.readCount()).isEqualTo(6);
                            q.clear();
                            q.find(id);
                            assertThat(q.readCount()).isEqualTo(7);
                            q.named(id);
                            q.named(id);
                            assertThat(q.readCount()).isEqualTo(8);
                            assertThat(ctx.getBean(CacheManager.class).getCacheNames())
                                    .contains("messages", "unknown", "named");
                        });
    }

    @Test
    void ttlDefaultsOverridesAndRealExpiration() {
        caching()
                .withPropertyValues(
                        "platform.redis.cache.default-ttl=250ms",
                        "platform.redis.cache.regions.messages.ttl=1200ms")
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            String id = key();
                            q.find(id);
                            q.unknown(id);
                            var nativeRedis = ctx.getBean(StringRedisTemplate.class);
                            assertThat(
                                            nativeRedis.getExpire(
                                                    physical(ctx, "messages", id),
                                                    TimeUnit.MILLISECONDS))
                                    .isBetween(500L, 1200L);
                            assertThat(
                                            nativeRedis.getExpire(
                                                    physical(ctx, "unknown", id),
                                                    TimeUnit.MILLISECONDS))
                                    .isBetween(1L, 250L);
                            await().atMost(Duration.ofSeconds(2))
                                    .untilAsserted(
                                            () ->
                                                    assertThat(
                                                                    nativeRedis.hasKey(
                                                                            physical(
                                                                                    ctx, "unknown",
                                                                                    id)))
                                                            .isFalse());
                            q.unknown(id);
                            assertThat(q.readCount()).isEqualTo(3);
                        });
        caching()
                .withPropertyValues("platform.redis.cache.default-ttl=0s")
                .run(ctx -> assertThat(ctx).hasFailed());
        caching()
                .withPropertyValues("platform.redis.cache.regions.messages.ttl=-1s")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void typedCollectionsOptionalNullAndExceptions() {
        caching()
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            String id = key();
                            assertThat(q.list(id)).isEqualTo(q.list(id));
                            assertThat(q.map(id)).isEqualTo(q.map(id));
                            assertThat(q.optional(id)).isEqualTo(q.optional(id));
                            assertThat(q.readCount()).isEqualTo(3);
                            assertThat(q.nullable(id)).isNull();
                            assertThat(q.nullable(id)).isNull();
                            assertThat(q.readCount()).isEqualTo(5);
                            assertThat(q.syncnull(id)).isNull();
                            assertThat(q.syncnull(id)).isNull();
                            assertThat(q.readCount()).isEqualTo(7);
                            assertThatThrownBy(() -> q.failure(id))
                                    .isInstanceOf(UnsupportedOperationException.class);
                            assertThatThrownBy(() -> q.failure(id))
                                    .isInstanceOf(UnsupportedOperationException.class);
                            assertThat(q.readCount()).isEqualTo(9);
                            String json =
                                    ctx.getBean(StringRedisTemplate.class)
                                            .opsForValue()
                                            .get(physical(ctx, "map", id));
                            assertThat(json).doesNotContain("@class", "java.util", "Message");
                        });
    }

    @Test
    void tenantUserLocaleRegionEnvironmentAndServiceIsolation() {
        caching()
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            String id = key();
                            q.secure("t1", "u1", "pt", id);
                            q.secure("t1", "u1", "pt", id);
                            q.secure("t2", "u1", "pt", id);
                            q.secure("t1", "u2", "pt", id);
                            q.secure("t1", "u1", "en", id);
                            assertThat(q.readCount()).isEqualTo(4);
                            q.find(id);
                            q.unknown(id);
                            assertThat(q.readCount()).isEqualTo(6);
                            caching()
                                    .withPropertyValues("platform.redis.cache.environment=prod")
                                    .run(
                                            other -> {
                                                other.getBean(Queries.class).find(id);
                                                assertThat(other.getBean(Queries.class).readCount())
                                                        .isEqualTo(1);
                                            });
                            caching()
                                    .withPropertyValues("spring.application.name=another-service")
                                    .run(
                                            other -> {
                                                other.getBean(Queries.class).find(id);
                                                assertThat(other.getBean(Queries.class).readCount())
                                                        .isEqualTo(1);
                                            });
                        });
    }

    @Test
    void transactionCommitRollbackAndEviction() {
        caching()
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            String id = key();
                            var tx = new TransactionTemplate(new LocalTransactions());
                            tx.executeWithoutResult(
                                    status -> {
                                        q.find(id);
                                        status.setRollbackOnly();
                                    });
                            q.find(id);
                            assertThat(q.readCount()).isEqualTo(2);
                            tx.executeWithoutResult(
                                    status -> {
                                        q.evict(id);
                                        status.setRollbackOnly();
                                    });
                            q.find(id);
                            assertThat(q.readCount()).isEqualTo(2);
                            tx.executeWithoutResult(status -> q.evict(id));
                            q.find(id);
                            assertThat(q.readCount()).isEqualTo(3);
                            String another = key();
                            tx.executeWithoutResult(status -> q.find(another));
                            q.find(another);
                            assertThat(q.readCount()).isEqualTo(4);
                            assertThatThrownBy(
                                            () -> tx.executeWithoutResult(status -> q.sync(key())))
                                    .isInstanceOf(
                                            br.com.portalmanager.platform.library.messaging
                                                    .exception.PlatformConfigurationException
                                                    .class);
                        });
    }

    @Test
    void syncAvoidsConcurrentMissStampede() {
        caching()
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            String id = key();
                            try (var executor = Executors.newFixedThreadPool(8)) {
                                var tasks = new ArrayList<Callable<Message>>();
                                for (int n = 0; n < 8; n++) tasks.add(() -> q.sync(id));
                                var results = executor.invokeAll(tasks);
                                for (var result : results)
                                    assertThat(result.get().version()).isEqualTo(1);
                            }
                            assertThat(q.readCount()).isEqualTo(1);
                        });
    }

    @Test
    void failurePolicyBypassesOnlyReadsAndNeverRepositoryOrExplicitWrites() {
        caching()
                .withPropertyValues(
                        "spring.data.redis.port=1",
                        "spring.data.redis.connect-timeout=50ms",
                        "spring.data.redis.timeout=50ms")
                .run(
                        ctx ->
                                assertThatThrownBy(() -> ctx.getBean(Queries.class).find(key()))
                                        .isInstanceOf(ApiException.class));
        caching()
                .withPropertyValues(
                        "platform.redis.cache.failure-policy=FAIL_OPEN_READ",
                        "spring.data.redis.port=1",
                        "spring.data.redis.connect-timeout=50ms",
                        "spring.data.redis.timeout=50ms")
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            assertThat(q.find(key())).isNotNull();
                            assertThatThrownBy(() -> q.update(key()))
                                    .isInstanceOf(ApiException.class);
                            assertThatThrownBy(() -> q.evict(key()))
                                    .isInstanceOf(ApiException.class);
                            assertThatThrownBy(() -> q.failure(key()))
                                    .isInstanceOf(UnsupportedOperationException.class);
                        });
    }

    @Test
    void corruptedPayloadIsStandardizedAndCannotSelectArbitraryTypes() {
        caching()
                .run(
                        ctx -> {
                            var q = ctx.getBean(Queries.class);
                            String id = key();
                            var redis = ctx.getBean(StringRedisTemplate.class);
                            redis.opsForValue().set(physical(ctx, "messages", id), "INVALID_JSON");
                            assertThatThrownBy(() -> q.find(id))
                                    .isInstanceOf(ApiException.class)
                                    .hasMessage("redis.serialization");
                            String metadata =
                                    new tools.jackson.databind.json.JsonMapper()
                                            .writeValueAsString(
                                                    Map.of("@class", "java.lang.Runtime"));
                            redis.opsForValue().set(physical(ctx, "messages", id), metadata);
                            try {
                                assertThat(q.find(id)).isInstanceOf(Message.class);
                            } catch (ApiException rejected) {
                                assertThat(rejected.getMessageKey())
                                        .isEqualTo("redis.serialization");
                            }
                        });
    }

    public static class Gate {
        private final CountDownLatch entered = new CountDownLatch(1),
                release = new CountDownLatch(1);

        @Cacheable(cacheNames = "gate", key = "#id", sync = true)
        public Message blocked(String id) {
            entered.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new Message(id, 1);
        }

        public boolean entered() throws InterruptedException {
            return entered.await(2, TimeUnit.SECONDS);
        }

        public void release() {
            release.countDown();
        }
    }

    @Test
    void locksAreAlsoIsolatedAcrossServices() {
        caching()
                .withBean(Gate.class, Gate::new)
                .run(
                        first -> {
                            caching()
                                    .withPropertyValues("spring.application.name=other-service")
                                    .withBean(Gate.class, Gate::new)
                                    .run(
                                            second -> {
                                                var a = first.getBean(Gate.class);
                                                var b = second.getBean(Gate.class);
                                                try (var executor =
                                                        Executors.newFixedThreadPool(2)) {
                                                    var one =
                                                            executor.submit(
                                                                    () -> a.blocked("same"));
                                                    assertThat(a.entered()).isTrue();
                                                    var two =
                                                            executor.submit(
                                                                    () -> b.blocked("same"));
                                                    try {
                                                        assertThat(b.entered()).isTrue();
                                                    } finally {
                                                        a.release();
                                                        b.release();
                                                    }
                                                    assertThat(one.get()).isNotNull();
                                                    assertThat(two.get()).isNotNull();
                                                }
                                            });
                        });
    }

    public static class Conflict {
        @Cacheable("messages")
        public String different(String id) {
            return id;
        }
    }

    @Test
    void incompatibleRegionTypesFailWithPlatformConfigurationError() {
        caching().withBean(Conflict.class, Conflict::new).run(ctx -> assertThat(ctx).hasFailed());
    }

    static class LocalTransactions extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction() {
            return new Object();
        }

        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        protected void doCommit(DefaultTransactionStatus status) {}

        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
