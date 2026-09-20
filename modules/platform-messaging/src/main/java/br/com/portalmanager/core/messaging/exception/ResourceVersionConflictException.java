package br.com.portalmanager.core.messaging.exception;

import br.com.portalmanager.core.messaging.message.PlatformMessageKeys;

public class ResourceVersionConflictException extends ConflictException {

    public ResourceVersionConflictException() {
        super(PlatformMessageKeys.RESOURCE_VERSION_CONFLICT);
    }

    public ResourceVersionConflictException(Throwable cause) {
        super(PlatformMessageKeys.RESOURCE_VERSION_CONFLICT, cause);
    }
}
