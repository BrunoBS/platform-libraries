package br.com.portalmanager.platform.library.observability.logging;

/**
 * Explicitly marks a value for partial masking when it is rendered in a log message.
 *
 * <p>For example: {@code logger.info("Credential: {}", SensitiveValue.of(credential))}.</p>
 */
public final class SensitiveValue {

    private static volatile boolean maskingEnabled = true;

    private final String value;

    private SensitiveValue(Object value) {
        this.value = value == null ? null : String.valueOf(value);
    }

    public static SensitiveValue of(Object value) {
        return new SensitiveValue(value);
    }

    /** Configured by the observability logging properties. */
    public static void setMaskingEnabled(boolean enabled) {
        maskingEnabled = enabled;
    }

    @Override
    public String toString() {
        if (value == null) {
            return "null";
        }
        if (!maskingEnabled || value.isEmpty()) {
            return value;
        }

        int[] codePoints = value.codePoints().toArray();
        if (codePoints.length <= 2) {
            return "*".repeat(codePoints.length);
        }

        int visibleAtEachEnd = codePoints.length >= 7 ? 3 : 1;
        StringBuilder masked = new StringBuilder(value.length());
        for (int index = 0; index < visibleAtEachEnd; index++) {
            masked.appendCodePoint(codePoints[index]);
        }
        masked.append("*".repeat(codePoints.length - (visibleAtEachEnd * 2)));
        for (int index = codePoints.length - visibleAtEachEnd; index < codePoints.length; index++) {
            masked.appendCodePoint(codePoints[index]);
        }
        return masked.toString();
    }
}
