package br.com.portalmanager.platform.library.messaging.resolver;

import br.com.portalmanager.platform.library.messaging.model.ApiMessage;

import java.util.Locale;

public interface ApiMessageResolver {
    ApiMessage resolve(String messageKey, Locale locale);
}
