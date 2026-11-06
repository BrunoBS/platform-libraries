package br.com.portalmanager.platform.library.objectstorage.api.contract;

import java.time.Instant;

/**
 * A stored version returned by the selected provider's version history. {@code deleteMarker} is true only when
 * the provider returns a delete marker as a version entry.
 */
public record ObjectVersion(
        ObjectReference reference,
        String eTag,
        long sizeBytes,
        Instant lastModified,
        boolean current,
        boolean deleteMarker) { }
