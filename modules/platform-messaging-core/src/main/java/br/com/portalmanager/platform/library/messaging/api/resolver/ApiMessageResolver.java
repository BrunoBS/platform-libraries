package br.com.portalmanager.platform.library.messaging.api.resolver;

import br.com.portalmanager.platform.library.messaging.api.model.ApiMessage;

import java.util.Locale;

public interface ApiMessageResolver {
    ApiMessage resolve(String messageKey, Locale locale);
}
