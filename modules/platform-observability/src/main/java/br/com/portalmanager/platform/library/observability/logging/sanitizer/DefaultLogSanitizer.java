package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DefaultLogSanitizer implements LogSanitizer {

    private static final Set<String> DEFAULT_SENSITIVE_JSON_FIELDS = Set.of(
            "password",
            "passwd",
            "pwd",
            "token",
            "access_token",
            "refresh_token",
            "authorization",
            "secret",
            "client_secret",
            "api_key",
            "apikey"
    );

    private static final Pattern CPF = Pattern.compile("\\b\\d{3}[.-]?\\d{3}[.-]?\\d{3}[.-]?\\d{2}\\b");
    private static final Pattern CARD = Pattern.compile("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    public static final DefaultLogSanitizer INSTANCE = new DefaultLogSanitizer(Set.of());

    private final Pattern sensitiveJsonFieldPrefix;

    private DefaultLogSanitizer(Collection<String> additionalSensitiveFields) {
        TreeSet<String> fields = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        fields.addAll(DEFAULT_SENSITIVE_JSON_FIELDS);
        if (additionalSensitiveFields != null) {
            additionalSensitiveFields.stream()
                    .filter(field -> field != null && !field.isBlank())
                    .map(String::trim)
                    .forEach(fields::add);
        }

        String alternatives = fields.stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        this.sensitiveJsonFieldPrefix = Pattern.compile(
                "(?i)(\\\"(?:" + alternatives + ")\\\"\\s*:\\s*)"
        );
    }

    static DefaultLogSanitizer withAdditionalSensitiveFields(Collection<String> additionalSensitiveFields) {
        return new DefaultLogSanitizer(additionalSensitiveFields);
    }

    @Override
    public String sanitize(String value) {
        String sanitized = sanitizeSensitiveFields(value);
        if (sanitized == null || sanitized.isBlank()) {
            return sanitized;
        }

        sanitized = CPF.matcher(sanitized).replaceAll("***.***.***-**");
        sanitized = CARD.matcher(sanitized).replaceAll("****-****-****-****");
        return maskEmails(sanitized);
    }

    private String sanitizeSensitiveFields(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return maskSensitiveJsonFields(value);
    }

    private String maskSensitiveJsonFields(String value) {
        Matcher matcher = sensitiveJsonFieldPrefix.matcher(value);
        StringBuilder sanitized = new StringBuilder(value.length());
        int copiedUntil = 0;
        int scannedUntil = 0;
        StringState state = new StringState(false, false);

        while (matcher.find()) {
            if (matcher.start() < copiedUntil) {
                continue;
            }

            state = scanStringState(value, scannedUntil, matcher.start(), state);
            if (state.insideString()) {
                state = scanStringState(value, matcher.start(), matcher.end(), state);
                scannedUntil = matcher.end();
                continue;
            }

            int valueStart = matcher.end();
            int valueEnd = findJsonValueEnd(value, valueStart);
            sanitized.append(value, copiedUntil, valueStart).append("\"***\"");
            copiedUntil = valueEnd;
            scannedUntil = valueEnd;
            state = new StringState(false, false);
        }

        sanitized.append(value, copiedUntil, value.length());
        return sanitized.toString();
    }

    private static StringState scanStringState(String value, int start, int end, StringState initialState) {
        boolean insideString = initialState.insideString();
        boolean escaped = initialState.escaped();

        for (int index = start; index < end; index++) {
            char character = value.charAt(index);
            if (insideString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    insideString = false;
                }
            } else if (character == '"') {
                insideString = true;
            }
        }

        return new StringState(insideString, escaped);
    }

    private static int findJsonValueEnd(String value, int start) {
        int valueStart = start;
        while (valueStart < value.length() && Character.isWhitespace(value.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart == value.length()) {
            return valueStart;
        }

        char first = value.charAt(valueStart);
        if (first == '"') {
            return findQuotedValueEnd(value, valueStart);
        }
        if (first == '{' || first == '[') {
            return findCompositeValueEnd(value, valueStart);
        }

        int end = valueStart;
        while (end < value.length()) {
            char character = value.charAt(end);
            if (character == ',' || character == '}' || character == ']' || Character.isWhitespace(character)) {
                break;
            }
            end++;
        }
        return end;
    }

    private static int findQuotedValueEnd(String value, int start) {
        boolean escaped = false;
        for (int index = start + 1; index < value.length(); index++) {
            char character = value.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                return index + 1;
            }
        }
        return value.length();
    }

    private static int findCompositeValueEnd(String value, int start) {
        char[] openings = new char[value.length() - start];
        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;

        for (int index = start; index < value.length(); index++) {
            char character = value.charAt(index);
            if (insideString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    insideString = false;
                }
                continue;
            }

            if (character == '"') {
                insideString = true;
            } else if (character == '{' || character == '[') {
                openings[depth++] = character;
            } else if (character == '}' || character == ']') {
                if (depth == 0) {
                    return index;
                }
                char opening = openings[--depth];
                if ((opening == '{' && character != '}') || (opening == '[' && character != ']')) {
                    return value.length();
                }
                if (depth == 0) {
                    return index + 1;
                }
            }
        }
        return value.length();
    }

    private String maskEmails(String value) {
        Matcher matcher = EMAIL.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String email = matcher.group();
            int at = email.indexOf('@');
            String masked = at > 1 ? email.charAt(0) + "****" + email.substring(at) : "****@****.***";
            matcher.appendReplacement(result, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private record StringState(boolean insideString, boolean escaped) {}
}
