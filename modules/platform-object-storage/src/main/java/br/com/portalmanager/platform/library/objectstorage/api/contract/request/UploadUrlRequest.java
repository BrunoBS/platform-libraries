package br.com.portalmanager.platform.library.objectstorage.api.contract.request;

import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;

import java.time.Duration;

/** Request for a temporary URL that uploads a new object or replaces its current contents. */
public record UploadUrlRequest(ObjectReference reference, Duration validity, ObjectHeaders headers) {
    public UploadUrlRequest {
        ContractValidation.requireReference(reference);
        SignedUrlValidation.requireValidity(validity);
        if (reference.versionId() != null) throw ContractValidation.invalid("versionId");
        if (headers == null) headers = ObjectHeaders.empty();
    }
}
