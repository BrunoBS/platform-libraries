package br.com.portalmanager.core.messaging.resolver;

import br.com.portalmanager.core.messaging.model.ApiMessage;

import java.util.Locale;

public interface ApiMessageResolver {
    ApiMessage resolve(String messageKey, Locale locale);
}
