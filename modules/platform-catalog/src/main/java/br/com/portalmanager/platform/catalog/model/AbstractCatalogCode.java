package br.com.portalmanager.platform.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

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

    private static String requireValid(String value) {
        if (!isValidFormat(value)) {
            throw new IllegalArgumentException("Invalid catalog code: " + value);
        }
        return value;
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
