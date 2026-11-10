package br.com.portalmanager.platform.library.messaging.internal.repository;

import br.com.portalmanager.platform.library.messaging.api.model.ApiMessage;
import br.com.portalmanager.platform.library.messaging.api.repository.ApiMessageRepository;

import java.util.Locale;
import java.util.Optional;

public final class NoOpApiMessageRepository implements ApiMessageRepository {

    @Override
    public Optional<ApiMessage> find(String messageKey, Locale locale) {
        return Optional.empty();
    }
}
