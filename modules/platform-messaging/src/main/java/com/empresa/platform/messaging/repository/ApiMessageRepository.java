package com.empresa.platform.messaging.repository;

import com.empresa.platform.messaging.model.ApiMessage;

import java.util.Locale;
import java.util.Optional;

public interface ApiMessageRepository {
    Optional<ApiMessage> find(String messageKey, Locale locale);
}
