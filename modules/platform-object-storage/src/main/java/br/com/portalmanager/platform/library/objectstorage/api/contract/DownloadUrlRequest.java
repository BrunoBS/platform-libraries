package br.com.portalmanager.platform.library.objectstorage.api.contract;

import java.time.Duration;

/** Request for a temporary URL that downloads one object, optionally at an exact version. */
public record DownloadUrlRequest(ObjectReference reference, Duration validity) {
    public DownloadUrlRequest {
        ContractValidation.requireReference(reference);
        SignedUrlValidation.requireValidity(validity);
    }
}
