package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.time.Duration;

final class SignedUrlValidation {
    private static final Duration MAX_VALIDITY = Duration.ofDays(7).minusMinutes(10);

    private SignedUrlValidation() { }

    static void requireValidity(Duration validity) {
        if (validity == null || validity.isZero() || validity.isNegative() || validity.compareTo(MAX_VALIDITY) > 0) {
            throw ObjectStorageValidation.invalid("validity");
        }
    }
}
