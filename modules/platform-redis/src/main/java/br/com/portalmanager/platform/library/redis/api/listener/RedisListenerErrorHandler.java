package br.com.portalmanager.platform.library.redis.api.listener;

import br.com.portalmanager.platform.library.messaging.api.exception.ApiException;

/** Receives sanitized errors without payloads, keys, channels or original exception messages. */
@FunctionalInterface
public interface RedisListenerErrorHandler {
    void onError(String transport, ApiException failure);
}
