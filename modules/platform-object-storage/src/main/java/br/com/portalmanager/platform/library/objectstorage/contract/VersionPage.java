package br.com.portalmanager.platform.library.objectstorage.contract;

import java.util.List;

public record VersionPage(List<ObjectVersion> versions, String continuationToken) {
    public VersionPage {
        versions = List.copyOf(versions);
    }
}
