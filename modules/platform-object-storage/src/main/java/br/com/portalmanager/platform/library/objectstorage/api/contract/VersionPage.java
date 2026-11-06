package br.com.portalmanager.platform.library.objectstorage.api.contract;

import java.util.List;

/** One version-listing page. The continuation token is opaque and is reused with the same destination and key. */
public record VersionPage(List<ObjectVersion> versions, String continuationToken) {
    public VersionPage {
        if (versions == null) throw ContractValidation.invalid("versions");
        if (versions.stream().anyMatch(version -> version == null)) throw ContractValidation.invalid("versions");
        versions = List.copyOf(versions);
    }
}
