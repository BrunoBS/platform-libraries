package com.empresa.platform.crud.version;

import com.empresa.platform.messaging.exception.ResourceVersionConflictException;

import java.util.Objects;

public final class OptimisticLockSupport {

    private OptimisticLockSupport() {
    }

    public static void validateCreate(Object resource) {
        if (resource instanceof VersionedResource versionedResource
                && versionedResource.version() != null) {
            throw new ResourceVersionConflictException();
        }
    }

    public static void validate(Object entity, Object resource) {
        if (entity instanceof OptimisticLockable lockable
                && resource instanceof VersionedResource versionedResource) {
            validate(lockable, versionedResource);
        }
    }

    public static void validate(
            OptimisticLockable entity,
            VersionedResource resource) {

        Long requestedVersion = resource.version();

        if (requestedVersion == null
                || !Objects.equals(entity.getVersion(), requestedVersion)) {
            throw new ResourceVersionConflictException();
        }
    }
}
