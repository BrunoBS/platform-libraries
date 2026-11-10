package br.com.portalmanager.platform.library.redis.api.stream;

/** Stream ID and decoded payload. No driver types escape this contract. */
public record RedisStreamRecord<T>(String id, T payload) {}
