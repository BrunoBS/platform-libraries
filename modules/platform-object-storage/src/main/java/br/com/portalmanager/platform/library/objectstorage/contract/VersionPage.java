package br.com.portalmanager.platform.library.objectstorage.contract;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;
import java.util.List;

public record VersionPage(List<ObjectVersion> versions, String continuationToken) {
    public VersionPage {
        if (versions == null) throw ObjectStorageValidation.invalid("versions");
        if (versions.stream().anyMatch(version -> version == null)) throw ObjectStorageValidation.invalid("versions");
        versions = List.copyOf(versions);
    }
}
