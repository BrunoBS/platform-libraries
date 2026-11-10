package br.com.portalmanager.platform.library.objectstorage.internal.error;

import br.com.portalmanager.platform.library.messaging.api.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.api.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.api.exception.ForbiddenException;
import br.com.portalmanager.platform.library.messaging.api.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.api.exception.UnauthorizedException;
import br.com.portalmanager.platform.library.objectstorage.internal.message.ObjectStorageMessageKeys;
import java.io.IOException;
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
        assertMapped(409, ApiException.class, cause, ObjectStorageMessageKeys.OPERATION_FAILED);
        assertMapped(412, ApiException.class, cause, ObjectStorageMessageKeys.OPERATION_FAILED);
        assertMapped(408, ApiException.class, cause, ObjectStorageMessageKeys.OPERATION_TIMEOUT);
        assertMapped(503, ApiException.class, cause, ObjectStorageMessageKeys.TRANSIENT_FAILURE);
    }

    @Test
    void shouldMapOnlyProviderSpecificCreateConflictsAsObjectAlreadyExists() {
        RuntimeException cause = new RuntimeException("provider failure");

        assertConflict(ObjectStorageFailureMapper.fromAwsFailure(
                412, "PreconditionFailed", "upload object", true, cause), cause);
        assertConflict(ObjectStorageFailureMapper.fromAzureFailure(
                409, "BlobAlreadyExists", "upload object", true, cause), cause);

        RuntimeException awsConcurrentWrite = ObjectStorageFailureMapper.fromAwsFailure(
                409, "ConditionalRequestConflict", "upload object", true, cause);
        assertInstanceOf(ApiException.class, awsConcurrentWrite);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) awsConcurrentWrite).getMessageKey());

        RuntimeException unrelated = ObjectStorageFailureMapper.fromAzureFailure(
                409, "LeaseIdMissing", "delete object", false, cause);
        assertInstanceOf(ApiException.class, unrelated);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) unrelated).getMessageKey());
        assertEquals(cause, unrelated.getCause());

        RuntimeException wrongOperation = ObjectStorageFailureMapper.fromAzureFailure(
                409, "BlobAlreadyExists", "delete object", false, cause);
        assertInstanceOf(ApiException.class, wrongOperation);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) wrongOperation).getMessageKey());

        RuntimeException awsWrongOperation = ObjectStorageFailureMapper.fromAwsFailure(
                412, "PreconditionFailed", "delete object", false, cause);
        assertInstanceOf(ApiException.class, awsWrongOperation);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) awsWrongOperation).getMessageKey());

        RuntimeException preconditionFailure = ObjectStorageFailureMapper.fromAzureFailure(
                412, "ConditionNotMet", "upload object", true, cause);
        assertInstanceOf(ApiException.class, preconditionFailure);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) preconditionFailure).getMessageKey());
    }

    @Test
    void shouldNotClassifyCredentialOrInvalidClientFailuresAsTransient() {
        RuntimeException cause = new RuntimeException("invalid credentials");
        RuntimeException mapped = ObjectStorageFailureMapper.fromFailure("upload object", cause);

        assertInstanceOf(ApiException.class, mapped);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) mapped).getMessageKey());
        assertEquals(cause, mapped.getCause());
    }

    @Test
    void shouldClassifyClientTimeoutAsTimeout() {
        SocketTimeoutException cause = new SocketTimeoutException("timed out");
        RuntimeException mapped = ObjectStorageFailureMapper.fromFailure("upload object", cause);

        assertInstanceOf(ApiException.class, mapped);
        assertEquals(ObjectStorageMessageKeys.OPERATION_TIMEOUT, ((ApiException) mapped).getMessageKey());
        assertEquals(cause, mapped.getCause());
    }

    @Test
    void shouldClassifyOnlyKnownProviderIoFailuresAsTransient() {
        RuntimeException unclassified = ObjectStorageFailureMapper.fromFailure(
                "download object", new RuntimeException("invalid provider response"));
        assertInstanceOf(ApiException.class, unclassified);
        assertEquals(ObjectStorageMessageKeys.OPERATION_FAILED, ((ApiException) unclassified).getMessageKey());

        RuntimeException ioFailure = ObjectStorageFailureMapper.fromFailure(
                "download object", new RuntimeException("connection reset", new IOException("reset")));
        assertInstanceOf(ApiException.class, ioFailure);
        assertEquals(ObjectStorageMessageKeys.TRANSIENT_FAILURE, ((ApiException) ioFailure).getMessageKey());
    }

    private void assertMapped(int status, Class<? extends RuntimeException> type, Throwable cause, String key) {
        RuntimeException mapped = ObjectStorageFailureMapper.fromHttpStatus(status, "upload object", cause);

        assertInstanceOf(type, mapped);
        assertInstanceOf(ApiException.class, mapped);
        assertEquals(cause, mapped.getCause());
        assertEquals(key, ((ApiException) mapped).getMessageKey());
    }

    private void assertConflict(RuntimeException mapped, Throwable cause) {
        assertInstanceOf(ConflictException.class, mapped);
        assertEquals(cause, mapped.getCause());
        assertEquals(ObjectStorageMessageKeys.OBJECT_ALREADY_EXISTS, ((ApiException) mapped).getMessageKey());
    }
}
