package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** Temporary URL and the HTTP details the caller must use with it. */
public record SignedUrlResult(
        URI url,
        SignedUrlOperation operation,
        Instant expiresAt,
        Map<String, String> requiredHeaders) {

    public SignedUrlResult {
        if (url == null || !url.isAbsolute()) throw ObjectStorageValidation.invalid("url");
        if (operation == null) throw ObjectStorageValidation.invalid("operation");
        if (expiresAt == null) throw ObjectStorageValidation.invalid("expiresAt");
        requiredHeaders = requiredHeaders == null ? Map.of() : Map.copyOf(requiredHeaders);
    }
}
