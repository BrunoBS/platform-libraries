package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SignedUrlRequestTest {

    @Test
    void shouldRejectInvalidValidityAndUploadOfAnExactVersion() {
        ObjectReference current = ObjectReference.current("audit", "feature.json");
        ObjectReference version = new ObjectReference("audit", "feature.json", "version-1");

        assertThrows(ValidationException.class, () -> new SignedUrlRequest(
                current, SignedUrlOperation.DOWNLOAD, Duration.ZERO, null));
        assertThrows(ValidationException.class, () -> new SignedUrlRequest(
                current, SignedUrlOperation.DOWNLOAD, Duration.ofDays(7), null));
        assertThrows(ValidationException.class, () -> new SignedUrlRequest(
                version, SignedUrlOperation.UPLOAD, Duration.ofMinutes(5), null));
        assertThrows(ValidationException.class, () -> new SignedUrlRequest(
                current, SignedUrlOperation.DOWNLOAD, Duration.ofMinutes(5), new ObjectHeaders("application/json", null, null, null, null)));
    }
}
