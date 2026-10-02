package br.com.portalmanager.platform.library.schemavalidation.message;

public final class SchemaValidationMessageKeys {

    private static final String PREFIX = "schemavalidation.";

    public static final String INVALID = PREFIX + "invalid";

    private SchemaValidationMessageKeys() {
    }

    public static String fromKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return INVALID;
        }

        String normalized = keyword
                .trim()
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replace('_', '-')
                .toLowerCase(java.util.Locale.ROOT);

        if (!normalized.matches("[a-z0-9][a-z0-9-]*")) {
            return INVALID;
        }

        return PREFIX + normalized;
    }
}
