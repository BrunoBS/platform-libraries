package br.com.portalmanager.platform.library.audit.config;

import br.com.portalmanager.platform.library.audit.message.AuditTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;

public final class AuditPropertiesValidator {

    public AuditPropertiesValidator(PlatformAuditProperties properties) {
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.SERVICE_NAME_REQUIRED);
        }
    }
}
