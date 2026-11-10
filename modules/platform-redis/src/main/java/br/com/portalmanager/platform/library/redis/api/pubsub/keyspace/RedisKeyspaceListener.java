package br.com.portalmanager.platform.library.redis.api.pubsub.keyspace;

@FunctionalInterface
public interface RedisKeyspaceListener {
    void onMessage(RedisKeyspaceEvent event);
}
