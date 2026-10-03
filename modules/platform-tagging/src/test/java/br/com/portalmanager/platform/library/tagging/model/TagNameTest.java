package br.com.portalmanager.platform.library.tagging.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagNameTest {

    @Test
    void shouldNormalizeAtTheBoundary() {
        assertThat(TagName.of("  My   TAG  ").value()).isEqualTo("my-tag");
    }

    @Test
    void shouldRejectNullBlankAndOversizedNames() {
        assertThatThrownBy(() -> TagName.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagName.of("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagName.of("a".repeat(TagName.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("150");
    }

    @Test
    void shouldAcceptMaximumLength() {
        assertThat(TagName.of("a".repeat(TagName.MAX_LENGTH)).value()).hasSize(TagName.MAX_LENGTH);
    }
}
