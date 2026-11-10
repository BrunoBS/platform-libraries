package br.com.portalmanager.platform.library.messaging.api.exception;

import br.com.portalmanager.platform.library.messaging.api.message.PlatformMessageKeys;

public class ResourceVersionConflictException extends ConflictException {

    public ResourceVersionConflictException() {
        super(PlatformMessageKeys.RESOURCE_VERSION_CONFLICT);
    }

    public ResourceVersionConflictException(Throwable cause) {
        super(PlatformMessageKeys.RESOURCE_VERSION_CONFLICT, cause);
    }
}
