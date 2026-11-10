package br.com.portalmanager.platform.library.messaging.api.repository;

import br.com.portalmanager.platform.library.messaging.api.model.ApiMessage;

import java.util.Locale;
import java.util.Optional;

public interface ApiMessageRepository {
    Optional<ApiMessage> find(String messageKey, Locale locale);
}
