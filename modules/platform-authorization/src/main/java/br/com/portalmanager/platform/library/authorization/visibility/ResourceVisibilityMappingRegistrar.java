package br.com.portalmanager.platform.library.authorization.visibility;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizerGroup;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Attaches an entity-scoped platform visibility filter to entities that declare
 * exactly one persistent String field annotated with {@link AuthorizerGroup}.
 */
public final class ResourceVisibilityMappingRegistrar {

    private ResourceVisibilityMappingRegistrar() {
    }

    public static int register(Metadata metadata) {
        int registered = 0;

        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            Class<?> mappedClass = entityBinding.getMappedClass();
            if (mappedClass == null) {
                continue;
            }

            List<Field> authorizerFields = findAuthorizerFields(mappedClass);
            if (authorizerFields.isEmpty()) {
                continue;
            }
            if (authorizerFields.size() != 1) {
                throw new IllegalStateException(
                        "Entity " + mappedClass.getName()
                                + " must declare exactly one @AuthorizerGroup field"
                );
            }

            Field authorizerField = authorizerFields.getFirst();
            if (authorizerField.getType() != String.class) {
                throw new IllegalStateException(
                        "@AuthorizerGroup field " + mappedClass.getName() + "." + authorizerField.getName()
                                + " must be java.lang.String"
                );
            }

            String filterName = ResourceVisibilityFilterManager.filterName(mappedClass);
            boolean alreadyRegistered = entityBinding.getFilters().stream()
                    .anyMatch(filter -> filterName.equals(filter.getName()));
            if (alreadyRegistered) {
                continue;
            }

            String condition = resolveCondition(entityBinding, mappedClass, authorizerField);

            entityBinding.addFilter(
                    filterName,
                    condition,
                    true,
                    Map.of(),
                    Map.of());
            registered++;
        }

        return registered;
    }

    static List<Class<?>> findResourceTypes(Metadata metadata) {
        List<Class<?>> resourceTypes = new ArrayList<>();

        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            Class<?> mappedClass = entityBinding.getMappedClass();
            if (mappedClass != null && !findAuthorizerFields(mappedClass).isEmpty()) {
                resourceTypes.add(mappedClass);
            }
        }

        return List.copyOf(resourceTypes);
    }

    private static String resolveCondition(
            PersistentClass entityBinding,
            Class<?> mappedClass,
            Field authorizerField
    ) {
        Property property;
        try {
            property = entityBinding.getProperty(authorizerField.getName());
        } catch (MappingException exception) {
            throw new IllegalStateException(
                    "@AuthorizerGroup field " + mappedClass.getName() + "." + authorizerField.getName()
                            + " is not a persistent Hibernate property",
                    exception
            );
        }

        List<Selectable> selectables = property.getSelectables();
        if (property.getColumnSpan() != 1
                || selectables.size() != 1
                || !(selectables.getFirst() instanceof Column column)) {
            throw new IllegalStateException(
                    "@AuthorizerGroup field " + mappedClass.getName() + "." + authorizerField.getName()
                            + " must map to exactly one physical column"
            );
        }

        return column.getQuotedName()
                + " in (:" + ResourceVisibilityFilterManager.PARAMETER_NAME + ")";
    }

    private static List<Field> findAuthorizerFields(Class<?> mappedClass) {
        List<Field> result = new ArrayList<>();

        for (Class<?> type = mappedClass;
             type != null && type != Object.class;
             type = type.getSuperclass()) {

            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(AuthorizerGroup.class)) {
                    result.add(field);
                }
            }
        }

        return result;
    }
}
