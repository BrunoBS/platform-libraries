package br.com.portalmanager.platform.library.objectstorage.internal;

import br.com.portalmanager.platform.library.objectstorage.exception.ObjectStorageValidation;

/** Applies destination prefix/suffix rules and rejects ambiguous logical keys. */
public final class ObjectStorageKey {
    private ObjectStorageKey() { }

    public static String resolve(String prefix, String suffix, String key) {
        validatePath(key, "key");
        String normalizedPrefix = normalizePrefix(prefix);
        if (!normalizedPrefix.isEmpty()
                && (key.equals(normalizedPrefix) || key.startsWith(normalizedPrefix + "/"))) {
            throw ObjectStorageValidation.invalid("key");
        }
        String normalizedSuffix = suffix == null ? "" : suffix;
        if (!normalizedSuffix.isEmpty() && key.endsWith(normalizedSuffix)) {
            throw ObjectStorageValidation.invalid("key");
        }
        String physicalKey = normalizedPrefix.isEmpty() ? key : normalizedPrefix + "/" + key;
        return physicalKey + normalizedSuffix;
    }

    public static String logical(String prefix, String suffix, String physicalKey) {
        String relative = stripPrefix(normalizePrefix(prefix), physicalKey);
        if (relative == null) return null;
        String normalizedSuffix = suffix == null ? "" : suffix;
        if (!normalizedSuffix.isEmpty()) {
            if (!relative.endsWith(normalizedSuffix)) return null;
            relative = relative.substring(0, relative.length() - normalizedSuffix.length());
        }
        return relative;
    }

    public static String resolvePrefix(String configuredPrefix, String requestedPrefix) {
        if (requestedPrefix == null || requestedPrefix.isEmpty()) return normalizePrefix(configuredPrefix);
        boolean trailingSlash = requestedPrefix.endsWith("/");
        String logicalPrefix = trailingSlash ? requestedPrefix.substring(0, requestedPrefix.length() - 1) : requestedPrefix;
        validatePath(logicalPrefix, "prefix");
        String normalizedPrefix = normalizePrefix(configuredPrefix);
        if (!normalizedPrefix.isEmpty()
                && (logicalPrefix.equals(normalizedPrefix) || logicalPrefix.startsWith(normalizedPrefix + "/"))) {
            throw ObjectStorageValidation.invalid("prefix");
        }
        String physicalPrefix = normalizedPrefix.isEmpty() ? logicalPrefix : normalizedPrefix + "/" + logicalPrefix;
        return trailingSlash ? physicalPrefix + "/" : physicalPrefix;
    }

    public static void validateConfiguredPrefix(String prefix) {
        normalizePrefix(prefix);
    }

    public static void validateConfiguredSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty()) return;
        if (suffix.isBlank() || suffix.contains("/") || suffix.contains("\\") || suffix.equals(".") || suffix.equals("..")) {
            throw ObjectStorageValidation.invalid("suffix");
        }
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return "";
        String normalized = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        validatePath(normalized, "prefix");
        return normalized;
    }

    private static String stripPrefix(String prefix, String physicalKey) {
        if (prefix.isEmpty()) return physicalKey;
        String normalizedPrefix = prefix + "/";
        return physicalKey.startsWith(normalizedPrefix) ? physicalKey.substring(normalizedPrefix.length()) : null;
    }

    private static void validatePath(String value, String field) {
        if (value == null || value.isBlank() || value.startsWith("/")
                || value.contains("\\") || value.contains("//")) {
            throw ObjectStorageValidation.invalid(field);
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw ObjectStorageValidation.invalid(field);
            }
        }
    }
}
