package br.com.portalmanager.platform.library.objectstorage.api.contract.request;

import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.WriteMode;

import java.time.Duration;

/**
 * Request for a temporary upload URL. Uploads are create-only by default; use {@link WriteMode#OVERWRITE}
 * when replacing the current object is intentional.
 */
public record UploadUrlRequest(
        ObjectReference reference,
        Duration validity,
        ObjectHeaders headers,
        WriteMode writeMode) {

    public UploadUrlRequest(ObjectReference reference, Duration validity, ObjectHeaders headers) {
        this(reference, validity, headers, WriteMode.CREATE_ONLY);
    }

    public UploadUrlRequest {
        ContractValidation.requireReference(reference);
        SignedUrlValidation.requireValidity(validity);
        if (reference.versionId() != null) throw ContractValidation.invalid("versionId");
        if (headers == null) headers = ObjectHeaders.empty();
        if (writeMode == null) throw ContractValidation.invalid("writeMode");
    }
}
