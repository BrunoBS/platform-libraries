package br.com.portalmanager.platform.library.messaging.exception;

import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;

public class ResourceVersionConflictException extends ConflictException {

    public ResourceVersionConflictException() {
        super(PlatformMessageKeys.RESOURCE_VERSION_CONFLICT);
    }

    public ResourceVersionConflictException(Throwable cause) {
        super(PlatformMessageKeys.RESOURCE_VERSION_CONFLICT, cause);
    }
}
