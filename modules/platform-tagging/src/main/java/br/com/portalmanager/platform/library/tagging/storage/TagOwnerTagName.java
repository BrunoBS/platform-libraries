package br.com.portalmanager.platform.library.tagging.storage;

import br.com.portalmanager.platform.library.tagging.exception.TaggingTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.tagging.model.TagName;

/**
 * Scalar projection used by batch tag lookups without loading the owner entity.
 */
public record TagOwnerTagName(String ownerIdentifier, TagName tagName) {

    public TagOwnerTagName {
        if (ownerIdentifier == null || ownerIdentifier.isBlank() || tagName == null) {
            throw new PlatformConfigurationException(TaggingTechnicalErrors.PERSISTED_STATE_INVALID);
        }
    }
}
