package br.com.portalmanager.platform.library.redis.pubsub.keyspace;

@FunctionalInterface
public interface RedisKeyspaceListener {
    void onMessage(RedisKeyspaceEvent event);
}
