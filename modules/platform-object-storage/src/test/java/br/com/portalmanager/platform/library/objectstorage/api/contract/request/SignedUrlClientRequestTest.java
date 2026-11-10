package br.com.portalmanager.platform.library.objectstorage.api.contract.request;

import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.WriteMode;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.SignedUrl;

import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignedUrlClientRequestTest {

    @Test
    void shouldRejectInvalidValidityAndVersionSpecificUpload() {
        ObjectReference current = ObjectReference.current("audit", "feature.json");
        ObjectReference version = new ObjectReference("audit", "feature.json", "version-1");

        assertThrows(ValidationException.class, () -> new DownloadUrlRequest(current, Duration.ZERO));
        assertThrows(ValidationException.class, () -> new DownloadUrlRequest(current, Duration.ofDays(7)));
        assertThrows(ValidationException.class, () -> new UploadUrlRequest(version, Duration.ofMinutes(5), null));
    }

    @Test
    void shouldDefaultUploadHeadersAndCopyRequiredHeaders() {
        UploadUrlRequest request = new UploadUrlRequest(
                ObjectReference.current("audit", "feature.json"), Duration.ofMinutes(5), null);
        Map<String, String> headers = Map.of("Content-Type", "application/json");
        SignedUrl url = new SignedUrl(URI.create("https://storage.example/feature.json"), Instant.now(), headers);

        assertEquals(ObjectHeaders.empty(), request.headers());
        assertEquals(WriteMode.CREATE_ONLY, request.writeMode());
        assertEquals(headers, url.requiredHeaders());

        UploadUrlRequest overwrite = new UploadUrlRequest(
                ObjectReference.current("audit", "feature.json"),
                Duration.ofMinutes(5),
                ObjectHeaders.empty(),
                WriteMode.OVERWRITE);
        assertEquals(WriteMode.OVERWRITE, overwrite.writeMode());
        assertThrows(ValidationException.class, () -> new UploadUrlRequest(
                ObjectReference.current("audit", "feature.json"),
                Duration.ofMinutes(5),
                ObjectHeaders.empty(),
                null));
    }
}
