package com.empresa.platform.catalog.model;

import java.util.Arrays;
import java.util.List;

/**
 * Optional contract for catalogs constrained by a Java enum.
 * Managed catalogs do not need to implement or depend on this type.
 */
public interface CatalogEnum<T extends Enum<T>> {

    default List<String> getOptions() {
        Class<?> enumClass = getClass();
        Object[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return List.of();
        }
        return Arrays.stream(constants)
                .map(value -> ((Enum<?>) value).name())
                .toList();
    }

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
        E[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return "";
        }
        return String.join(", ", constants[0].getOptions());
    }
}
