package br.com.portalmanager.core.messaging.repository;

import br.com.portalmanager.core.messaging.model.ApiMessage;

import java.util.Locale;
import java.util.Optional;

public final class NoOpApiMessageRepository implements ApiMessageRepository {

    @Override
    public Optional<ApiMessage> find(String messageKey, Locale locale) {
        return Optional.empty();
    }
}
