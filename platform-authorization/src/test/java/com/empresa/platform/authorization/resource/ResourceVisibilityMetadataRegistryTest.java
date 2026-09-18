package com.empresa.platform.authorization.resource;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceVisibilityMetadataRegistryTest {

    @Test
    void shouldResolveMetadataCaseInsensitively() {
        ResourceVisibilityMetadataRegistry registry = new ResourceVisibilityMetadataRegistry(
                List.of(new ResourceVisibilityMetadata("accounts", "authorizer_group"))
        );

        assertThat(registry.findByTableName("ACCOUNTS"))
                .contains(new ResourceVisibilityMetadata("accounts", "authorizer_group"));
    }

    @Test
    void shouldRejectDuplicateTableMetadataCaseInsensitively() {
        assertThatThrownBy(() -> new ResourceVisibilityMetadataRegistry(List.of(
                new ResourceVisibilityMetadata("accounts", "authorizer_group"),
                new ResourceVisibilityMetadata("ACCOUNTS", "other_column")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate resource visibility metadata");
    }
}
