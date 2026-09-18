package com.empresa.platform.tagging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagNormalizerTest {

    @Test
    void shouldNormalizeTag() {
        assertThat(TagNormalizer.normalize("  Minha   Tag  ")).isEqualTo("minha-tag");
    }

    @Test
    void shouldReturnNullForBlankValues() {
        assertThat(TagNormalizer.normalize("   ")).isNull();
        assertThat(TagNormalizer.normalize(null)).isNull();
    }
}
