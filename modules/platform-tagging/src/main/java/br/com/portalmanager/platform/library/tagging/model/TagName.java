package br.com.portalmanager.platform.library.tagging.model;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.tagging.message.TaggingMessageKeys;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record TagName(String value) {

    public static final int MAX_LENGTH = 150;

    public TagName {
        value = normalize(value);
        if (value == null || value.length() > MAX_LENGTH) {
            throw invalidTagName();
        }
    }

    public static TagName of(String value) {
        return new TagName(value);
    }

    /**
     * Returns an empty Optional when the input is absent or normalizes to an empty name.
     * A normalized name longer than MAX_LENGTH is still rejected as invalid input.
     */
    public static Optional<TagName> ofIfPresent(String value) {
        String normalized = normalize(value);
        return normalized == null
                ? Optional.empty()
                : Optional.of(new TagName(normalized));
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

    private static ValidationException invalidTagName() {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail("tag", TaggingMessageKeys.TAG_NAME_INVALID))
        );
    }

    @Override
    public String toString() {
        return value;
    }
}
