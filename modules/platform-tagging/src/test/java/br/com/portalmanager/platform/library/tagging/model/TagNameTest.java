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
    void shouldRemoveDiacriticsAndSpecialCharactersWhilePreservingLettersAndDigits() {
        assertThat(TagName.of("  Revisão #1 + urgente!  ").value()).isEqualTo("revisao-1-urgente");
    }

    @Test
    void shouldStripDiacriticsFromCanonicallyEquivalentUnicodeLetters() {
        assertThat(TagName.of("Café").value()).isEqualTo("cafe");
        assertThat(TagName.of("Cafe\u0301").value()).isEqualTo("cafe");
    }

    @Test
    void shouldRejectNullBlankSpecialCharacterOnlyAndOversizedNames() {
        assertThatThrownBy(() -> TagName.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagName.of("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagName.of("!!!")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TagName.of("a".repeat(TagName.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("150");
    }

    @Test
    void shouldPersistTheCanonicalTagName() {
        TagNameConverter converter = new TagNameConverter();

        assertThat(converter.convertToDatabaseColumn(TagName.of("C++")))
                .isEqualTo("c");
    }

    @Test
    void shouldNormalizeSpecialCharactersWhenReadingTheTagName() {
        TagNameConverter converter = new TagNameConverter();

        assertThat(converter.convertToEntityAttribute("C++"))
                .isEqualTo(TagName.of("c"));
    }

    @Test
    void shouldAcceptMaximumLength() {
        assertThat(TagName.of("a".repeat(TagName.MAX_LENGTH)).value()).hasSize(TagName.MAX_LENGTH);
    }
}
