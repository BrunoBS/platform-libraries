package br.com.portalmanager.platform.library.tagging.model;

import java.text.Normalizer;
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
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}\\s-]", "")
                .replaceAll("\\s+", "-");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC);
        return normalized.isBlank() ? null : normalized;
    }

    @Override
    public String toString() {
        return value;
    }
}
