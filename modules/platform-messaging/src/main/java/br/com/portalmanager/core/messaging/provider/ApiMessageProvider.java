package br.com.portalmanager.core.messaging.provider;

import br.com.portalmanager.core.messaging.model.ApiMessage;

import java.util.Locale;
import java.util.Optional;

/**
 * Extension point for platform modules that provide built-in fallback messages.
 * Repository/cache definitions keep precedence; providers are consulted only
 * when no external message is configured.
 */
public interface ApiMessageProvider {

    Optional<ApiMessage> find(String key, Locale locale);
}
