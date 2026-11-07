package br.com.portalmanager.platform.library.objectstorage.internal.error;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ForbiddenException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.UnauthorizedException;
import br.com.portalmanager.platform.library.objectstorage.internal.message.ObjectStorageMessageKeys;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public final class ObjectStorageFailureMapper {
    private ObjectStorageFailureMapper() { }

    public static RuntimeException fromHttpStatus(int status, String operation, Throwable cause) {
        return switch (status) {
            case 404 -> new NotFoundException(ObjectStorageMessageKeys.OBJECT_NOT_FOUND, cause);
            case 401 -> new UnauthorizedException(ObjectStorageMessageKeys.UNAUTHORIZED, cause);
            case 403 -> new ForbiddenException(ObjectStorageMessageKeys.ACCESS_DENIED, cause);
            case 408, 504 -> new ApiException(
                    ObjectStorageMessageKeys.OPERATION_TIMEOUT, Map.of("0", operation), cause);
            case 429, 500, 502, 503 -> new ApiException(
                    ObjectStorageMessageKeys.TRANSIENT_FAILURE, Map.of("0", operation), cause);
            default -> new ApiException(ObjectStorageMessageKeys.OPERATION_FAILED, Map.of("0", operation), cause);
        };
    }

    public static RuntimeException fromAwsFailure(
            int status, String providerCode, String operation, boolean createOnly, Throwable cause) {
        if (createOnly && "upload object".equals(operation)
                && status == 412 && "PreconditionFailed".equals(providerCode)) {
            return new ConflictException(ObjectStorageMessageKeys.OBJECT_ALREADY_EXISTS, cause);
        }
        return fromHttpStatus(status, operation, cause);
    }

    public static RuntimeException fromAzureFailure(
            int status, String providerCode, String operation, boolean createOnly, Throwable cause) {
        if (createOnly && "upload object".equals(operation)
                && status == 409 && "BlobAlreadyExists".equals(providerCode)) {
            return new ConflictException(ObjectStorageMessageKeys.OBJECT_ALREADY_EXISTS, cause);
        }
        return fromHttpStatus(status, operation, cause);
    }

    public static RuntimeException fromFailure(String operation, Throwable cause) {
        if (hasCause(cause, TimeoutException.class) || hasCause(cause, SocketTimeoutException.class)) {
            return new ApiException(ObjectStorageMessageKeys.OPERATION_TIMEOUT, Map.of("0", operation), cause);
        }
        if (hasCause(cause, IOException.class)) {
            return new ApiException(ObjectStorageMessageKeys.TRANSIENT_FAILURE, Map.of("0", operation), cause);
        }
        return new ApiException(ObjectStorageMessageKeys.OPERATION_FAILED, Map.of("0", operation), cause);
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }
}
