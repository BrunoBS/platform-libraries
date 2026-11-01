package br.com.portalmanager.platform.library.tagging.internal;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagNameNormalizerTest {

    @Test
    void shouldNormalizeAtTheBoundary() {
        assertThat(TagNameNormalizer.normalizeRequired("  My   TAG  ")).isEqualTo("my-tag");
    }

    @Test
    void shouldRemoveDiacriticsAndSpecialCharactersWhilePreservingLettersAndDigits() {
        assertThat(TagNameNormalizer.normalizeRequired("  Revisão #1 + urgente!  "))
                .isEqualTo("revisao-1-urgente");
    }

    @Test
    void shouldStripDiacriticsFromCanonicallyEquivalentUnicodeLetters() {
        assertThat(TagNameNormalizer.normalizeRequired("Café")).isEqualTo("cafe");
        assertThat(TagNameNormalizer.normalizeRequired("Cafe\\u0301")).isEqualTo("cafe");
    }

    @Test
    void shouldIgnoreAbsentAndEmptyNamesAfterNormalization() {
        assertThat(TagNameNormalizer.normalizeIfPresent(null)).isEmpty();
        assertThat(TagNameNormalizer.normalizeIfPresent("   ")).isEmpty();
        assertThat(TagNameNormalizer.normalizeIfPresent("!!!")).isEmpty();
        assertThat(TagNameNormalizer.normalizeIfPresent(" \\u0301 ")).isEmpty();
    }

    @Test
    void shouldRejectInvalidRequiredNames() {
        assertThatThrownBy(() -> TagNameNormalizer.normalizeRequired(null))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TagNameNormalizer.normalizeRequired("   "))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TagNameNormalizer.normalizeRequired("!!!"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TagNameNormalizer.normalizeRequired("a".repeat(TagNameNormalizer.MAX_LENGTH + 1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectNamesAboveMaximumLengthWhenPresent() {
        assertThatThrownBy(() -> TagNameNormalizer.normalizeIfPresent(
                "a".repeat(TagNameNormalizer.MAX_LENGTH + 1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldAcceptMaximumLength() {
        assertThat(TagNameNormalizer.normalizeRequired("a".repeat(TagNameNormalizer.MAX_LENGTH)))
                .hasSize(TagNameNormalizer.MAX_LENGTH);
    }
}
