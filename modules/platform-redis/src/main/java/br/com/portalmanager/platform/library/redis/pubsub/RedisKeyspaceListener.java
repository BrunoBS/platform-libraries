package br.com.portalmanager.platform.library.redis.pubsub;

@FunctionalInterface
public interface RedisKeyspaceListener {
    void onMessage(RedisKeyspaceEvent event);
}
