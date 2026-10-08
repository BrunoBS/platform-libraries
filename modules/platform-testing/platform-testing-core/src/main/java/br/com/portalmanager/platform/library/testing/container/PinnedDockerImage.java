package br.com.portalmanager.platform.library.testing.container;

/** Shared checks for explicit Docker image versions used by test fixtures. */
public final class PinnedDockerImage {

    /**
     * Parses a version-pinned image reference. Floating tags such as {@code latest}
     * are rejected so test behavior does not change without a source change.
     */
    public static String validatePinnedImage(String image) {
        if (image == null || image.isBlank()) {
            throw new IllegalArgumentException("Docker image must not be blank");
        }

        String value = image.trim();
        int lastSlash = value.lastIndexOf('/');
        int lastColon = value.lastIndexOf(':');
        boolean hasTag = lastColon > lastSlash;
        boolean hasDigest = value.contains("@sha256:");
        if (!hasTag && !hasDigest) {
            throw new IllegalArgumentException("Docker image must include an explicit tag or digest");
        }
        if (hasTag && "latest".equalsIgnoreCase(value.substring(lastColon + 1))) {
            throw new IllegalArgumentException("Floating Docker image tag 'latest' is not supported");
        }
        return value;
    }

    private PinnedDockerImage() {
    }
}
