package br.com.portalmanager.platform.library.catalog.model;

import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared value-object base for semantic catalog codes.
 *
 * <p>The concrete catalog type remains responsible for its semantic identity
 * while this class centralizes storage, format validation and value semantics.</p>
 */
@MappedSuperclass
public abstract class AbstractCatalogCode {

    private static final int MAX_LENGTH = 50;
    private static final Pattern CODE_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,49}$");

    @Column(name = "value", nullable = false, length = MAX_LENGTH)
    protected String value;

    protected AbstractCatalogCode() {
    }

    protected AbstractCatalogCode(String value) {
        this.value = requireValid(value);
    }

    protected AbstractCatalogCode(Enum<?> value) {
        this(value == null ? null : value.name());
    }

    public final String value() {
        return value;
    }

    public static boolean isValidFormat(String value) {
        return value != null && CODE_PATTERN.matcher(value).matches();
    }

    /**
     * Resolves a textual catalog code against the enum that defines the semantic
     * values accepted by a concrete CatalogCode VO. Invalid values are exposed
     * through the platform messaging validation contract instead of leaking an
     * {@link IllegalArgumentException} from {@link Enum#valueOf(Class, String)}.
     */
    protected static <E extends Enum<E>> E requireEnumValue(String value, Class<E> enumType) {
        if (value == null) {
            throw invalidCatalogCode(value);
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw invalidCatalogCode(value);
        }
    }

    private static String requireValid(String value) {
        if (!isValidFormat(value)) {
            throw new ValidationException(
                    PlatformMessageKeys.VALIDATION_FAILED,
                    List.of(new ValidationDetail(
                            "code",
                            CatalogMessageKeys.CODE_INVALID_FORMAT,
                            Map.of("0", value == null ? "" : value)
                    ))
            );
        }
        return value;
    }

    private static ValidationException invalidCatalogCode(String value) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(
                        "code",
                        CatalogMessageKeys.CODE_NOT_ALLOWED,
                        Map.of("0", value == null ? "" : value)
                ))
        );
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AbstractCatalogCode that = (AbstractCatalogCode) other;
        return Objects.equals(value, that.value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), value);
    }

    @Override
    public final String toString() {
        return value;
    }
}
