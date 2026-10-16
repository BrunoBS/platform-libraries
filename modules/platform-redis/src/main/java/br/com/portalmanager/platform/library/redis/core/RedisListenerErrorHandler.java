package br.com.portalmanager.platform.library.redis.core;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;

/** Receives sanitized errors without payloads, keys, channels or original exception messages. */
@FunctionalInterface
public interface RedisListenerErrorHandler {
    void onError(String transport, ApiException failure);
}
