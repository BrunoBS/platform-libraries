package br.com.portalmanager.platform.library.catalog.model;

import java.util.Arrays;

/**
 * Marker contract for catalogs constrained by a Java enum.
 */
public interface CatalogEnum<T extends Enum<T>> {

    static <E extends Enum<E>> E from(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(item -> item.name().equals(value))
                .findFirst()
                .orElse(null);
    }

    static <E extends Enum<E> & CatalogEnum<E>> String getOptionsValid(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}
