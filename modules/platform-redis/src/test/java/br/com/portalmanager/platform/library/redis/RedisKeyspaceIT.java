package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.redis.annotation.RedisKeyspaceSubscribe;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.internal.RedisPubSubLifecycle;
import br.com.portalmanager.platform.library.redis.pubsub.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.*;

class RedisKeyspaceIT extends RedisIntegrationSupport {
    @RedisKeyspaceSubscribe(patterns = {"__keyevent@0__:expired"})
    static class Listener implements RedisKeyspaceListener {
        final BlockingQueue<RedisKeyspaceEvent> events = new LinkedBlockingQueue<>();

        public void onMessage(RedisKeyspaceEvent event) {
            events.add(event);
        }
    }

    @Test
    void notificationOptInDoesNotConfigureTheServer() {
        var listener = new Listener();
        context()
                .withBean(Listener.class, () -> listener)
                .run(
                        ctx ->
                                assertThat(ctx.getBean(RedisPubSubLifecycle.class).isRunning())
                                        .isFalse());
        context()
                .withPropertyValues("platform.redis.keyspace-enabled=true")
                .withBean(Listener.class, () -> listener)
                .run(
                        ctx -> {
                            var template = ctx.getBean(StringRedisTemplate.class);
                            template.execute(
                                    (RedisCallback<Void>)
                                            connection -> {
                                                connection
                                                        .serverCommands()
                                                        .setConfig("notify-keyspace-events", "Ex");
                                                return null;
                                            });
                            try {
                                String key = key();
                                ctx.getBean(RedisStore.class)
                                        .set(key, "value", Duration.ofMillis(50));
                                assertThat(listener.events.poll(3, TimeUnit.SECONDS))
                                        .isEqualTo(
                                                new RedisKeyspaceEvent(
                                                        "__keyevent@0__:expired",
                                                        "redis-test:" + key));
                            } finally {
                                template.execute(
                                        (RedisCallback<Void>)
                                                connection -> {
                                                    connection
                                                            .serverCommands()
                                                            .setConfig(
                                                                    "notify-keyspace-events", "");
                                                    return null;
                                                });
                            }
                        });
    }

    @Test
    void optInWithServerNotificationsDisabledDoesNotEnableThem() {
        var listener = new Listener();
        context()
                .withPropertyValues("platform.redis.keyspace-enabled=true")
                .withBean(Listener.class, () -> listener)
                .run(
                        ctx -> {
                            var store = ctx.getBean(RedisStore.class);
                            String key = key();
                            store.set(key, "value", Duration.ofMillis(40));
                            org.awaitility.Awaitility.await()
                                    .atMost(Duration.ofSeconds(2))
                                    .untilAsserted(() -> assertThat(store.exists(key)).isFalse());
                            assertThat(listener.events.poll(100, TimeUnit.MILLISECONDS)).isNull();
                        });
    }
}
