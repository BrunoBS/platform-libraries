package br.com.portalmanager.platform.library.objectstorage.contract;

import java.time.Instant;

public record ObjectVersion(
        ObjectReference reference,
        String eTag,
        long sizeBytes,
        Instant lastModified,
        boolean current,
        boolean deleteMarker) { }
