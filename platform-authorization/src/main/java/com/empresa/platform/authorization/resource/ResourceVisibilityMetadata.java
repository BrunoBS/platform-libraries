package com.empresa.platform.authorization.resource;

/**
 * Describes a database resource protected by resource visibility.
 */
public record ResourceVisibilityMetadata(String tableName, String visibilityColumn) {

    public ResourceVisibilityMetadata {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName must not be blank");
        }
        if (visibilityColumn == null || visibilityColumn.isBlank()) {
            throw new IllegalArgumentException("visibilityColumn must not be blank");
        }
    }
}
