package br.com.portalmanager.platform.library.objectstorage.provider.aws;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class S3VersionContinuationTokenTest {

    @Test
    void roundTripsKeysAndVersionsContainingNewlines() {
        String[] expected = {"folder/object\npart.json", "version\nid"};

        assertArrayEquals(expected, S3ObjectStorageClient.decodeToken(
                S3ObjectStorageClient.encodeToken(expected[0], expected[1])));
    }

    @Test
    void roundTripsNullVersionMarker() {
        assertArrayEquals(new String[] {"folder/object", null}, S3ObjectStorageClient.decodeToken(
                S3ObjectStorageClient.encodeToken("folder/object", null)));
    }

    @Test
    void acceptsContinuationTokensCreatedByThePreviousFormat() {
        String legacyToken = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("folder/object\nversion-id".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertArrayEquals(new String[] {"folder/object", "version-id"},
                S3ObjectStorageClient.decodeToken(legacyToken));
    }

    @Test
    void rejectsMalformedContinuationTokens() {
        assertThrows(ValidationException.class, () -> S3ObjectStorageClient.decodeToken("not-a-token"));
        assertThrows(ValidationException.class, () -> S3ObjectStorageClient.decodeToken(
                java.util.Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(new byte[] {0, 0, 0, 5, 1})));
    }
}
