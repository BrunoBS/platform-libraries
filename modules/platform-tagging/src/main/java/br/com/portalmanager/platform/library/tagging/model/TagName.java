package br.com.portalmanager.platform.library.tagging.model;

import java.util.Locale;
import java.util.Objects;

public record TagName(String value) implements Comparable<TagName> {

    public TagName {
        value = normalize(value);
        if (value == null) {
            throw new IllegalArgumentException("tag name must not be blank");
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
    public int compareTo(TagName other) {
        return value.compareTo(Objects.requireNonNull(other, "tag name must not be null").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
