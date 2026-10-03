package br.com.portalmanager.platform.library.catalog.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;

import java.util.Map;

/**
 * Technical failure inside the catalog capability.
 *
 * <p>Business validation continues to use the standard messaging exceptions.
 * This exception is reserved for unexpected infrastructure/mapping failures and
 * always preserves the original cause.</p>
 */
public final class CatalogTechnicalException extends ApiException {

    public CatalogTechnicalException(
            String messageKey,
            Map<String, Object> parameters,
            Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
