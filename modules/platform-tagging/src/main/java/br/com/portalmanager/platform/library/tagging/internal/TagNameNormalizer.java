package br.com.portalmanager.platform.library.tagging.internal;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.tagging.message.TaggingMessageKeys;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TagNameNormalizer {

    public static final int MAX_LENGTH = 150;

    private TagNameNormalizer() {
    }

    public static String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.length() > MAX_LENGTH) {
            throw invalidTagName();
        }
        return normalized;
    }

    public static Optional<String> normalizeIfPresent(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return Optional.empty();
        }
        if (normalized.length() > MAX_LENGTH) {
            throw invalidTagName();
        }
        return Optional.of(normalized);
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
}
