package br.com.portalmanager.platform.library.authorization.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizerGroupParserTest {

    @Test
    void shouldParseGroupWithProfile() {
        assertThat(AuthorizerGroupParser.parse("PM5-ENG-DEV_WSE"))
                .contains(new ParsedGroup(
                        "PM5-ENG-DEV_WSE",
                        "ENG",
                        "DEV",
                        "WSE"
                ));
    }

    @Test
    void shouldParseGroupWithoutProfile() {
        assertThat(AuthorizerGroupParser.parse("PM5-DEV_WSE"))
                .contains(new ParsedGroup(
                        "PM5-DEV_WSE",
                        null,
                        "DEV",
                        "WSE"
                ));
    }

    @Test
    void shouldIgnoreNonAuthorizerGroups() {
        assertThat(AuthorizerGroupParser.parseAll(Set.of(
                "USER",
                "TESTER",
                "PM5-NEG-ADM_CATALOG"
        ))).containsExactly(
                new ParsedGroup(
                        "PM5-NEG-ADM_CATALOG",
                        "NEG",
                        "ADM",
                        "CATALOG"
                )
        );
    }

    @Test
    void shouldIgnoreBlankOrInvalidGroup() {
        assertThat(AuthorizerGroupParser.parse(" ")).isEmpty();
        assertThat(AuthorizerGroupParser.parse("INVALID")).isEmpty();
    }
}
