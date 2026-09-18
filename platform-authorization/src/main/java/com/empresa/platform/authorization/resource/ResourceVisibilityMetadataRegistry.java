package com.empresa.platform.authorization.resource;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of relational resources whose rows are visibility protected.
 *
 * Applications extend the registry by declaring ResourceVisibilityMetadata beans.
 */
public class ResourceVisibilityMetadataRegistry {

    private final List<ResourceVisibilityMetadata> resources;
    private final Map<String, ResourceVisibilityMetadata> resourcesByTable;

    public ResourceVisibilityMetadataRegistry(Collection<ResourceVisibilityMetadata> resources) {
        this.resources = List.copyOf(resources);

        Map<String, ResourceVisibilityMetadata> indexed = new LinkedHashMap<>();
        for (ResourceVisibilityMetadata metadata : this.resources) {
            String key = normalize(metadata.tableName());
            ResourceVisibilityMetadata previous = indexed.putIfAbsent(key, metadata);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate resource visibility metadata for table: " + metadata.tableName()
                );
            }
        }
        this.resourcesByTable = Map.copyOf(indexed);
    }

    public List<ResourceVisibilityMetadata> resources() {
        return resources;
    }

    public Optional<ResourceVisibilityMetadata> findByTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(resourcesByTable.get(normalize(tableName)));
    }

    private String normalize(String tableName) {
        return tableName.toLowerCase(Locale.ROOT);
    }
}
