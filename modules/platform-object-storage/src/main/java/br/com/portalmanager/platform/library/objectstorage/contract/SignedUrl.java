package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** Temporary URL and any HTTP headers the caller must send with an upload. */
public record SignedUrl(URI url, Instant expiresAt, Map<String, String> requiredHeaders) {
    public SignedUrl {
        if (url == null || !url.isAbsolute()) throw ObjectStorageValidation.invalid("url");
        if (expiresAt == null) throw ObjectStorageValidation.invalid("expiresAt");
        requiredHeaders = requiredHeaders == null ? Map.of() : Map.copyOf(requiredHeaders);
    }
}
