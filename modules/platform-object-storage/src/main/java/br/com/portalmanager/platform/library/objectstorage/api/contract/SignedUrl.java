package br.com.portalmanager.platform.library.objectstorage.api.contract;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** Temporary URL and any HTTP headers the caller must send with an upload. */
public record SignedUrl(URI url, Instant expiresAt, Map<String, String> requiredHeaders) {
    public SignedUrl {
        if (url == null || !url.isAbsolute()) throw ContractValidation.invalid("url");
        if (expiresAt == null) throw ContractValidation.invalid("expiresAt");
        requiredHeaders = requiredHeaders == null ? Map.of() : Map.copyOf(requiredHeaders);
    }
}
