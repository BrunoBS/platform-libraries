package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.time.Duration;

/** Request to create a short-lived URL for one object and one operation. */
public record SignedUrlRequest(
        ObjectReference reference,
        SignedUrlOperation operation,
        Duration validity,
        ObjectHeaders headers) {

    public static final Duration MAX_VALIDITY = Duration.ofDays(7).minusMinutes(10);

    public SignedUrlRequest {
        ObjectStorageValidation.requireReference(reference);
        if (operation == null) throw ObjectStorageValidation.invalid("operation");
        if (validity == null || validity.isZero() || validity.isNegative() || validity.compareTo(MAX_VALIDITY) > 0) {
            throw ObjectStorageValidation.invalid("validity");
        }
        if (operation == SignedUrlOperation.UPLOAD && reference.versionId() != null) {
            throw ObjectStorageValidation.invalid("versionId");
        }
        if (operation == SignedUrlOperation.DOWNLOAD && headers != null && !headers.asHttpHeaders().isEmpty()) {
            throw ObjectStorageValidation.invalid("headers");
        }
        if (headers == null) headers = ObjectHeaders.empty();
    }
}
