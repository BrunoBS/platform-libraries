package br.com.portalmanager.platform.library.objectstorage.api.contract.response;

import java.util.List;

/** One version-listing page, ordered newest-first by last-modified time. The token is opaque and bound to destination/key. */
public record VersionPage(List<ObjectVersion> versions, String continuationToken) {
    public VersionPage {
        if (versions == null) throw ContractValidation.invalid("versions");
        if (versions.stream().anyMatch(version -> version == null)) throw ContractValidation.invalid("versions");
        versions = List.copyOf(versions);
    }
}
