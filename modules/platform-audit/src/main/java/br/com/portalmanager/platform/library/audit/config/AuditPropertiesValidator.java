package br.com.portalmanager.platform.library.audit.config;

import br.com.portalmanager.platform.library.audit.message.AuditTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;

public final class AuditPropertiesValidator {

    public AuditPropertiesValidator(PlatformAuditProperties properties) {
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.SERVICE_NAME_REQUIRED);
        }
        if (properties.getMaxEventSizeBytes() <= 0) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.EVENT_SIZE_LIMIT_INVALID);
        }
        if (properties.getMaxEventsPerInvocation() <= 0) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.EVENT_COUNT_LIMIT_INVALID);
        }
    }
}
