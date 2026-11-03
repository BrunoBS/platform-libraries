package br.com.portalmanager.platform.library.redis.pubsub.keyspace;

/** Raw native channel and UTF-8 body; these are not business events or JSON payloads. */
public record RedisKeyspaceEvent(String channel, String body) {}
