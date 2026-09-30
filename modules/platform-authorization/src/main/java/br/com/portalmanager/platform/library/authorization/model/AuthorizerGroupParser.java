package br.com.portalmanager.platform.library.authorization.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses platform authorization groups into the structured authorizer data used
 * by resource visibility.
 */
public final class AuthorizerGroupParser {

    private static final Pattern GROUP_PATTERN =
            Pattern.compile("^PM5-(?:(ENG|NEG)-)?(DEV|TST|ADM)_(.+)$");

    private AuthorizerGroupParser() {
    }

    public static Optional<ParsedGroup> parse(String group) {
        if (group == null || group.isBlank()) {
            return Optional.empty();
        }

        String normalized = group.trim();
        Matcher matcher = GROUP_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(new ParsedGroup(
                normalized,
                matcher.group(1),
                matcher.group(2),
                matcher.group(3)
        ));
    }

    public static Set<ParsedGroup> parseAll(Collection<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return Set.of();
        }

        Set<ParsedGroup> parsed = new LinkedHashSet<>();
        groups.forEach(group -> parse(group).ifPresent(parsed::add));
        return Set.copyOf(parsed);
    }
}
