package br.com.portalmanager.platform.library.objectstorage.api.contract;

import br.com.portalmanager.platform.library.objectstorage.internal.validation.ObjectStorageValidation;
import java.util.List;

public record ObjectPage(List<ObjectSummary> objects, String continuationToken) {
    public ObjectPage {
        if (objects == null) throw ObjectStorageValidation.invalid("objects");
        if (objects.stream().anyMatch(object -> object == null)) throw ObjectStorageValidation.invalid("objects");
        objects = List.copyOf(objects);
    }
}
