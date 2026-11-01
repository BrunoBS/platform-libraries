package br.com.portalmanager.platform.library.tagging.model;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
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
    void shouldIgnoreAbsentAndEmptyNamesAfterNormalization() {
        assertThat(TagName.ofIfPresent(null)).isEmpty();
        assertThat(TagName.ofIfPresent("   ")).isEmpty();
        assertThat(TagName.ofIfPresent("!!!")).isEmpty();
        assertThat(TagName.ofIfPresent(" \\u0301 ")).isEmpty();
    }

    @Test
    void shouldRejectInvalidDirectTagNameWithPlatformValidationException() {
        assertThatThrownBy(() -> TagName.of(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TagName.of("   ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TagName.of("!!!")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> TagName.of("a".repeat(TagName.MAX_LENGTH + 1)))
                .isInstanceOf(ValidationException.class);
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
    void shouldReportInvalidPersistedNamesWithPlatformConfigurationException() {
        TagNameConverter converter = new TagNameConverter();

        assertThatThrownBy(() -> converter.convertToEntityAttribute("!!!"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldAcceptMaximumLength() {
        assertThat(TagName.of("a".repeat(TagName.MAX_LENGTH)).value()).hasSize(TagName.MAX_LENGTH);
    }
}
