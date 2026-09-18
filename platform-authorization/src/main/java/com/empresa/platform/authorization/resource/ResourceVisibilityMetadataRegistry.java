package com.empresa.platform.authorization.resource;

import java.util.Collection;
import java.util.List;

/**
 * Registry of relational resources whose rows are visibility protected.
 */
public class ResourceVisibilityMetadataRegistry {

    private final List<ResourceVisibilityMetadata> resources;

    public ResourceVisibilityMetadataRegistry(Collection<ResourceVisibilityMetadata> resources) {
        this.resources = List.copyOf(resources);
    }

    public List<ResourceVisibilityMetadata> resources() {
        return resources;
    }
}
