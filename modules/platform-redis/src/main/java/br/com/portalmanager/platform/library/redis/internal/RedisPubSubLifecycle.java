package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.annotation.RedisKeyspaceSubscribe;
import br.com.portalmanager.platform.library.redis.annotation.RedisSubscribe;
import br.com.portalmanager.platform.library.redis.pubsub.RedisKeyspaceEvent;
import br.com.portalmanager.platform.library.redis.pubsub.RedisKeyspaceListener;
import br.com.portalmanager.platform.library.redis.pubsub.RedisMessageListener;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import tools.jackson.databind.JavaType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/** Creates no subscription or executor until at least one annotated listener exists. */
public final class RedisPubSubLifecycle implements SmartLifecycle {
    private final ConfigurableListableBeanFactory beans;
    private final RedisConnectionFactory connections;
    private final RedisJsonCodec codec;
    private final RedisAccess access;
    private final ListenerSupport support;
    private RedisMessageListenerContainer container;
    private ThreadPoolExecutor handlers;
    private ThreadPoolExecutor subscriptions;
    private volatile boolean running;

    public RedisPubSubLifecycle(
            ConfigurableListableBeanFactory beans,
            RedisConnectionFactory connections,
            RedisJsonCodec codec,
            RedisAccess access,
            ListenerSupport support) {
        this.beans = beans;
        this.connections = connections;
        this.codec = codec;
        this.access = access;
        this.support = support;
    }

    @Override
    public synchronized void start() {
        if (running) return;
        Map<MessageListener, Collection<? extends Topic>> registrations = new LinkedHashMap<>();
        registerBusinessListeners(registrations);
        if (access.properties().keyspaceEnabled()) registerKeyspaceListeners(registrations);
        if (registrations.isEmpty()) return;
        access.batch(registrations.size());
        handlers =
                support.executor(
                        "pubsub",
                        access.properties().listenerThreads(),
                        access.properties().listenerQueueCapacity());
        subscriptions = support.executor("subscription", 1, 16);
        container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connections);
        container.setTaskExecutor(handlers);
        container.setSubscriptionExecutor(subscriptions);
        container.setRecoveryBackoff(support.backoff());
        container.setErrorHandler(failure -> support.failed("pubsub", failure));
        container.setMessageListeners(registrations);
        container.setAutoStartup(false);
        try {
            container.afterPropertiesSet();
            container.start();
            running = true;
        } catch (RuntimeException failure) {
            stop();
            throw RedisErrors.translate(failure);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerBusinessListeners(
            Map<MessageListener, Collection<? extends Topic>> registrations) {
        for (var entry : beans.getBeansOfType(RedisMessageListener.class).entrySet()) {
            RedisSubscribe settings =
                    beans.findAnnotationOnBean(entry.getKey(), RedisSubscribe.class);
            if (settings == null) continue;
            List<Topic> topics = new ArrayList<>();
            for (String channel : settings.channels())
                topics.add(new ChannelTopic(access.text(channel)));
            for (String pattern : settings.patterns())
                topics.add(new PatternTopic(access.text(pattern)));
            requireTopics(topics);
            JavaType type = support.payloadType(entry.getValue(), RedisMessageListener.class);
            registrations.put(businessHandler(entry.getValue(), type), topics);
        }
    }

    private <T> MessageListener businessHandler(RedisMessageListener<T> listener, JavaType type) {
        return (message, pattern) -> {
            try {
                T payload = codec.decode(body(message), type);
                listener.onMessage(payload);
                support.processed();
            } catch (RuntimeException failure) {
                support.failed("pubsub", failure);
            }
        };
    }

    private void registerKeyspaceListeners(
            Map<MessageListener, Collection<? extends Topic>> registrations) {
        for (var entry : beans.getBeansOfType(RedisKeyspaceListener.class).entrySet()) {
            var settings = beans.findAnnotationOnBean(entry.getKey(), RedisKeyspaceSubscribe.class);
            if (settings == null) continue;
            List<Topic> topics = new ArrayList<>();
            for (String pattern : settings.patterns()) {
                access.text(pattern);
                if (!pattern.startsWith("__keyevent@") && !pattern.startsWith("__keyspace@"))
                    throw RedisErrors.configuration();
                topics.add(new PatternTopic(pattern));
            }
            requireTopics(topics);
            registrations.put(keyspaceHandler(entry.getValue()), topics);
        }
    }

    private MessageListener keyspaceHandler(RedisKeyspaceListener listener) {
        return (message, pattern) -> {
            try {
                listener.onMessage(
                        new RedisKeyspaceEvent(
                                new String(message.getChannel(), StandardCharsets.UTF_8),
                                body(message)));
                support.processed();
            } catch (RuntimeException failure) {
                support.failed("keyspace", failure);
            }
        };
    }

    private String body(Message message) {
        if (message.getBody().length > access.properties().maxPayloadBytes())
            throw RedisErrors.error("limit", null);
        return new String(message.getBody(), StandardCharsets.UTF_8);
    }

    private void requireTopics(List<Topic> topics) {
        if (topics.isEmpty()) throw RedisErrors.configuration();
        access.batch(topics.size());
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (container != null) {
            try {
                container.stop();
                container.destroy();
            } catch (Exception failure) {
                support.failed("pubsub", failure);
            } finally {
                container = null;
            }
        }
        support.shutdown(handlers);
        support.shutdown(subscriptions);
        handlers = null;
        subscriptions = null;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
