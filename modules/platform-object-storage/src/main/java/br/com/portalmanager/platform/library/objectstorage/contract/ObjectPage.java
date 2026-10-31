package br.com.portalmanager.platform.library.objectstorage.contract;

import java.util.List;

public record ObjectPage(List<ObjectSummary> objects, String continuationToken) {
    public ObjectPage {
        objects = List.copyOf(objects);
    }
}
