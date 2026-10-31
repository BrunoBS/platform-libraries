package br.com.portalmanager.platform.library.objectstorage.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ForbiddenException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.objectstorage.message.ObjectStorageMessageKeys;
import java.util.Map;

public final class ObjectStorageFailureMapper {
    private ObjectStorageFailureMapper() { }

    public static RuntimeException fromHttpStatus(int status, String operation, Throwable cause) {
        return switch (status) {
            case 404 -> new NotFoundException(ObjectStorageMessageKeys.OBJECT_NOT_FOUND, cause);
            case 403 -> new ForbiddenException(ObjectStorageMessageKeys.ACCESS_DENIED, cause);
            case 409, 412 -> new ConflictException(ObjectStorageMessageKeys.OBJECT_ALREADY_EXISTS, cause);
            case 408, 429, 500, 502, 503, 504 -> new ApiException(
                    ObjectStorageMessageKeys.TRANSIENT_FAILURE, Map.of("0", operation), cause);
            default -> new ApiException(ObjectStorageMessageKeys.OPERATION_FAILED, Map.of("0", operation), cause);
        };
    }

    public static RuntimeException fromProviderFailure(String operation, Throwable cause) {
        return new ApiException(ObjectStorageMessageKeys.TRANSIENT_FAILURE, Map.of("0", operation), cause);
    }
}
