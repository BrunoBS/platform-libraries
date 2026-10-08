package br.com.portalmanager.platform.library.tagging.storage;

import br.com.portalmanager.platform.library.tagging.model.TagName;

import java.util.Objects;

/**
 * Scalar projection used by batch tag lookups without loading the owner entity.
 */
public record TagOwnerTagName(String ownerIdentifier, TagName tagName) {

    public TagOwnerTagName {
        Objects.requireNonNull(ownerIdentifier, "owner identifier must not be null");
        if (ownerIdentifier.isBlank()) {
            throw new IllegalArgumentException("owner identifier must not be blank");
        }
        Objects.requireNonNull(tagName, "tag name must not be null");
    }
}
