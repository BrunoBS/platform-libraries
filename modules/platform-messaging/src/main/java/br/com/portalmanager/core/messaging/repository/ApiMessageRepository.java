package br.com.portalmanager.core.messaging.repository;

import br.com.portalmanager.core.messaging.model.ApiMessage;

import java.util.Locale;
import java.util.Optional;

public interface ApiMessageRepository {
    Optional<ApiMessage> find(String messageKey, Locale locale);
}
