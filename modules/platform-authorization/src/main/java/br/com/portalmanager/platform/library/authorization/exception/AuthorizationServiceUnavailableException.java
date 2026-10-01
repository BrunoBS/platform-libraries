package br.com.portalmanager.platform.library.authorization.exception;

import br.com.portalmanager.platform.library.authorization.message.AuthorizationTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;

/**
 * Signals a technical failure while communicating with the central Authorization API.
 */
public class AuthorizationServiceUnavailableException extends PlatformConfigurationException {

    public AuthorizationServiceUnavailableException(Throwable cause) {
        super(AuthorizationTechnicalErrors.SERVICE_UNAVAILABLE, cause);
    }
}
