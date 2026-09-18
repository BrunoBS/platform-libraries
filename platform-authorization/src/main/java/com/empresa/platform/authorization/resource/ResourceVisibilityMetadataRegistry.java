package com.empresa.platform.authorization.resource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry of relational resources whose rows are visibility protected.
 *
 * Applications extend the registry by declaring ResourceVisibilityMetadata beans.
 */
public class ResourceVisibilityMetadataRegistry {

    private final List<ResourceVisibilityMetadata> resources;

    public ResourceVisibilityMetadataRegistry(Collection<ResourceVisibilityMetadata> resources) {
        this.resources = List.copyOf(resources);
    }

    public List<ResourceVisibilityMetadata> resources() {
        return resources;
    }

    public Optional<ResourceVisibilityMetadata> findByTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Optional.empty();
        }

        return resources.stream()
                .filter(metadata -> metadata.tableName().equalsIgnoreCase(tableName))
                .findFirst();
    }
}
