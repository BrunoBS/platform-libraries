package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.time.Duration;

/** Request for a temporary URL that uploads a new object or replaces its current contents. */
public record UploadUrlRequest(ObjectReference reference, Duration validity, ObjectHeaders headers) {
    public UploadUrlRequest {
        ObjectStorageValidation.requireReference(reference);
        SignedUrlValidation.requireValidity(validity);
        if (reference.versionId() != null) throw ObjectStorageValidation.invalid("versionId");
        if (headers == null) headers = ObjectHeaders.empty();
    }
}
