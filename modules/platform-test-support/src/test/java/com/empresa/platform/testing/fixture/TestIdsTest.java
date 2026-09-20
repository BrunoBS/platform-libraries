package com.empresa.platform.testing.fixture;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestIdsTest {

    @Test
    void shouldCreateStableUuidFromSeed() {
        UUID first = TestIds.uuid("product-1");
        UUID second = TestIds.uuid("product-1");

        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldCreateDifferentUuidForDifferentSeed() {
        assertThat(TestIds.uuid("product-1"))
                .isNotEqualTo(TestIds.uuid("product-2"));
    }

    @Test
    void shouldRejectBlankSeed() {
        assertThatThrownBy(() -> TestIds.uuid(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seed");
    }
}
