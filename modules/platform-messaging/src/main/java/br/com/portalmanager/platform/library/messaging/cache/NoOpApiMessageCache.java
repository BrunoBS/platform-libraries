package br.com.portalmanager.platform.library.messaging.cache;

import br.com.portalmanager.platform.library.messaging.model.ApiMessage;

import java.util.Locale;
import java.util.Optional;

public final class NoOpApiMessageCache implements ApiMessageCache {

    @Override
    public Optional<ApiMessage> get(String messageKey, Locale locale) {
        return Optional.empty();
    }

    @Override
    public void put(ApiMessage message) {
        // Cache intentionally disabled.
    }

    @Override
    public void evict(String messageKey, Locale locale) {
        // Cache intentionally disabled.
    }
}
