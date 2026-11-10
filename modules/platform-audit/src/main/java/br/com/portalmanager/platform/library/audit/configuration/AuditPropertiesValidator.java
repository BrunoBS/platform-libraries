package br.com.portalmanager.platform.library.audit.configuration;

import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.internal.message.AuditTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.api.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;

public final class AuditPropertiesValidator {

    public AuditPropertiesValidator(
            PlatformAuditProperties properties,
            ObjectProvider<ObjectStorageProperties> objectStorageProperties,
            ObjectProvider<ObjectStorageClient> objectStorageClient) {
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.SERVICE_NAME_REQUIRED);
        }
        PlatformAuditProperties.Outbox outbox = properties.getOutbox();
        if (outbox.getDestination() == null || outbox.getDestination().isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.OUTBOX_DESTINATION_REQUIRED);
        }
        if (outbox.getBatchSize() < 1
                || outbox.getMaximumAttempts() < 1
                || outbox.getInitialAttempts() < 1
                || outbox.getInitialAttempts() > outbox.getMaximumAttempts()
                || outbox.getMaximumRecoveryDays() < 1
                || !positive(outbox.getShortRetryDelay())
                || !positive(outbox.getDailyRetryDelay())
                || !positive(outbox.getProcessingLease())
                || outbox.getKeyPrefix() == null
                || outbox.getKeyPrefix().isBlank()
                || outbox.getKeyPrefix().startsWith("/")) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.OUTBOX_CONFIGURATION_INVALID);
        }
        ObjectStorageProperties storageProperties = objectStorageProperties.getIfAvailable();
        ObjectStorageClient storageClient = objectStorageClient.getIfAvailable();
        if (storageProperties == null || storageClient == null) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.OBJECT_STORAGE_REQUIRED);
        }
        if (storageProperties.getDestinations() == null
                || !storageProperties.getDestinations().containsKey(outbox.getDestination())) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.OUTBOX_DESTINATION_REQUIRED);
        }
    }

    private boolean positive(Duration value) {
        return value != null && !value.isZero() && !value.isNegative();
    }
}
