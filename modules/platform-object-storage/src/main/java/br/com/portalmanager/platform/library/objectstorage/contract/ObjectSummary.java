package br.com.portalmanager.platform.library.objectstorage.contract;

import java.time.Instant;

/** Fields available from a provider's object listing without one properties request per object. */
public record ObjectSummary(ObjectReference reference, long sizeBytes, String eTag, Instant lastModified) { }
