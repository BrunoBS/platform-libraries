package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.time.Duration;

/** Request for a temporary URL that downloads one object, optionally at an exact version. */
public record DownloadUrlRequest(ObjectReference reference, Duration validity) {
    public DownloadUrlRequest {
        ObjectStorageValidation.requireReference(reference);
        SignedUrlValidation.requireValidity(validity);
    }
}
