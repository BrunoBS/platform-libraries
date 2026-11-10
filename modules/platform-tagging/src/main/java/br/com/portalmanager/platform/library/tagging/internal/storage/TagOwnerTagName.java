package br.com.portalmanager.platform.library.tagging.internal.storage;

import br.com.portalmanager.platform.library.tagging.internal.message.TaggingTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.api.exception.ApiException;
import br.com.portalmanager.platform.library.tagging.internal.TagNameNormalizer;
import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;

/**
 * Scalar projection used by batch tag lookups without loading the owner entity.
 */
public record TagOwnerTagName(String ownerIdentifier, String tagName) {

    public TagOwnerTagName {
        if (ownerIdentifier == null || ownerIdentifier.isBlank() || tagName == null) {
            throw new ApiException(TaggingTechnicalErrors.PERSISTED_STATE_INVALID);
        }
        try {
            tagName = TagNameNormalizer.normalizeRequired(tagName);
        } catch (ValidationException exception) {
            throw new ApiException(TaggingTechnicalErrors.PERSISTED_NAME_INVALID, exception);
        }
    }
}
