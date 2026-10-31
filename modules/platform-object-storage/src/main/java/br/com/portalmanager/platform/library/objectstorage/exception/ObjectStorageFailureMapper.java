package br.com.portalmanager.platform.library.objectstorage.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ForbiddenException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.UnauthorizedException;
import br.com.portalmanager.platform.library.objectstorage.message.ObjectStorageMessageKeys;
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
            case 409, 412 -> new ConflictException(ObjectStorageMessageKeys.OBJECT_ALREADY_EXISTS, cause);
            case 408, 504 -> new ApiException(
                    ObjectStorageMessageKeys.OPERATION_TIMEOUT, Map.of("0", operation), cause);
            case 429, 500, 502, 503 -> new ApiException(
                    ObjectStorageMessageKeys.TRANSIENT_FAILURE, Map.of("0", operation), cause);
            default -> new ApiException(ObjectStorageMessageKeys.OPERATION_FAILED, Map.of("0", operation), cause);
        };
    }

    public static RuntimeException fromProviderFailure(String operation, Throwable cause) {
        if (hasCause(cause, TimeoutException.class) || hasCause(cause, SocketTimeoutException.class)) {
            return new ApiException(ObjectStorageMessageKeys.OPERATION_TIMEOUT, Map.of("0", operation), cause);
        }
        return new ApiException(ObjectStorageMessageKeys.TRANSIENT_FAILURE, Map.of("0", operation), cause);
    }

    public static RuntimeException fromClientFailure(String operation, Throwable cause) {
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
