package br.com.portalmanager.platform.library.objectstorage.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ForbiddenException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.UnauthorizedException;
import br.com.portalmanager.platform.library.objectstorage.message.ObjectStorageMessageKeys;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ObjectStorageFailureMapperTest {
    @Test
    void shouldMapProviderStatusesToGoldenExceptionsAndPreserveCause() {
        RuntimeException cause = new RuntimeException("provider failure");

        assertMapped(404, NotFoundException.class, cause, ObjectStorageMessageKeys.OBJECT_NOT_FOUND);
        assertMapped(401, UnauthorizedException.class, cause, ObjectStorageMessageKeys.UNAUTHORIZED);
        assertMapped(403, ForbiddenException.class, cause, ObjectStorageMessageKeys.ACCESS_DENIED);
        assertMapped(409, ConflictException.class, cause, ObjectStorageMessageKeys.OBJECT_ALREADY_EXISTS);
        assertMapped(412, ConflictException.class, cause, ObjectStorageMessageKeys.OBJECT_ALREADY_EXISTS);
        assertMapped(408, ApiException.class, cause, ObjectStorageMessageKeys.OPERATION_TIMEOUT);
        assertMapped(503, ApiException.class, cause, ObjectStorageMessageKeys.TRANSIENT_FAILURE);
    }

    @Test
    void shouldNotClassifyCredentialOrInvalidClientFailuresAsTransient() {
        RuntimeException cause = new RuntimeException("invalid credentials");
        RuntimeException mapped = ObjectStorageFailureMapper.fromClientFailure("upload object", cause);

        assertInstanceOf(ApiException.class, mapped);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) mapped).getMessageKey());
        assertEquals(cause, mapped.getCause());
    }

    @Test
    void shouldClassifyClientTimeoutAsTimeout() {
        SocketTimeoutException cause = new SocketTimeoutException("timed out");
        RuntimeException mapped = ObjectStorageFailureMapper.fromClientFailure("upload object", cause);

        assertInstanceOf(ApiException.class, mapped);
        assertEquals(ObjectStorageMessageKeys.OPERATION_TIMEOUT, ((ApiException) mapped).getMessageKey());
        assertEquals(cause, mapped.getCause());
    }

    private void assertMapped(int status, Class<? extends RuntimeException> type, Throwable cause, String key) {
        RuntimeException mapped = ObjectStorageFailureMapper.fromHttpStatus(status, "upload object", cause);

        assertInstanceOf(type, mapped);
        assertInstanceOf(ApiException.class, mapped);
        assertEquals(cause, mapped.getCause());
        assertEquals(key, ((ApiException) mapped).getMessageKey());
    }
}
