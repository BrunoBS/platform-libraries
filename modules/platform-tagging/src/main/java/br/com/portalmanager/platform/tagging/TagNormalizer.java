package br.com.portalmanager.platform.tagging;

import java.util.Locale;

public final class TagNormalizer {

    private TagNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }
}
