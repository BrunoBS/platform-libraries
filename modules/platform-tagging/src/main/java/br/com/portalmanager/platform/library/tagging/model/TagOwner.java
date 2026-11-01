package br.com.portalmanager.platform.library.tagging.model;

import java.util.Collection;
import java.util.List;

public interface TagOwner {

    Long getId();

    String getIdentifier();

    default Collection<String> getSystemTags() {
        return List.of();
    }
}
