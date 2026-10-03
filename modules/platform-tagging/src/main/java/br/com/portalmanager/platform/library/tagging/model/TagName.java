package br.com.portalmanager.platform.library.tagging.model;

import java.util.Locale;

public record TagName(String value) {

    public static final int MAX_LENGTH = 150;

    public TagName {
        value = normalize(value);
        if (value == null) {
            throw new IllegalArgumentException("tag name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("tag name must not exceed " + MAX_LENGTH + " characters");
        }
    }

    public static TagName of(String value) {
        return new TagName(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    @Override
    public String toString() {
        return value;
    }
}
