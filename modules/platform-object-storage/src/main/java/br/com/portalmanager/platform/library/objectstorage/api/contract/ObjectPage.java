package br.com.portalmanager.platform.library.objectstorage.api.contract;

import java.util.List;

/** One listing page. The continuation token is opaque and is reused with the same destination and prefix. */
public record ObjectPage(List<ObjectSummary> objects, String continuationToken) {
    public ObjectPage {
        if (objects == null) throw ContractValidation.invalid("objects");
        if (objects.stream().anyMatch(object -> object == null)) throw ContractValidation.invalid("objects");
        objects = List.copyOf(objects);
    }
}
