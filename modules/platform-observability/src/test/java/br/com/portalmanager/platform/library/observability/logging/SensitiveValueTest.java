package br.com.portalmanager.platform.library.observability.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.MessageFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveValueTest {

    @BeforeEach
    void setUp() {
        SensitiveValue.setMaskingEnabled(true);
    }

    @AfterEach
    void tearDown() {
        SensitiveValue.setMaskingEnabled(true);
    }

    @Test
    void shouldMaskValuesWithAtMostTwoCharactersCompletely() {
        assertThat(SensitiveValue.of("a").toString()).isEqualTo("*");
        assertThat(SensitiveValue.of("ab").toString()).isEqualTo("**");
    }

    @Test
    void shouldKeepOneCharacterAtEachEndForValuesFromThreeToSixCharacters() {
        assertThat(SensitiveValue.of("abc").toString()).isEqualTo("a*c");
        assertThat(SensitiveValue.of("abcdef").toString()).isEqualTo("a****f");
    }

    @Test
    void shouldKeepThreeCharactersAtEachEndForValuesWithAtLeastSevenCharacters() {
        assertThat(SensitiveValue.of("abcdefg").toString()).isEqualTo("abc*efg");
        assertThat(SensitiveValue.of("abcdefghij").toString()).isEqualTo("abc****hij");
    }

    @Test
    void shouldUseTheSameMaskForUnicodeCodePoints() {
        assertThat(SensitiveValue.of("á🙂bcde").toString()).isEqualTo("á****e");
    }

    @Test
    void shouldRenderNullAndEmptyValuesWithoutThrowing() {
        assertThat(SensitiveValue.of(null).toString()).isEqualTo("null");
        assertThat(SensitiveValue.of("").toString()).isEmpty();
    }

    @Test
    void shouldReturnOriginalValueWhenMaskingIsDisabled() {
        SensitiveValue.setMaskingEnabled(false);

        assertThat(SensitiveValue.of("abcdefghij").toString()).isEqualTo("abcdefghij");
    }

    @Test
    void shouldUseTheSameMaskWhenRenderedAsAStandardMessageArgument() {
        String message = MessageFormatter.arrayFormat(
                "credential={}",
                new Object[]{SensitiveValue.of("abcdefghij")}
        ).getMessage();

        assertThat(message).isEqualTo("credential=abc****hij");
    }
}
