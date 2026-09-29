package br.com.portalmanager.platform.library.authorization.resource;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.PersistentClass;

import java.util.Map;

/**
 * POC bootstrap primitive that attaches the platform visibility filter to
 * authorizable entity mappings without requiring @Filter on domain entities.
 *
 * The remaining POC step is wiring this registrar into a Hibernate 7 bootstrap
 * extension point before the SessionFactory mapping model is finalized.
 */
public final class ResourceVisibilityMappingRegistrar {

    public static final String DEFAULT_CONDITION =
            "lower(authorizer_group) in (:" + ResourceVisibilityFilterManager.PARAMETER_NAME + ")";

    private ResourceVisibilityMappingRegistrar() {
    }

    public static int register(Metadata metadata) {
        return register(metadata, DEFAULT_CONDITION);
    }

    static int register(Metadata metadata, String condition) {
        int registered = 0;

        for (PersistentClass entityBinding : metadata.getEntityBindings()) {
            Class<?> mappedClass = entityBinding.getMappedClass();
            if (mappedClass == null || !AuthorizableResource.class.isAssignableFrom(mappedClass)) {
                continue;
            }

            boolean alreadyRegistered = entityBinding.getFilters().stream()
                    .anyMatch(filter -> ResourceVisibilityFilterManager.FILTER_NAME.equals(filter.getName()));

            if (!alreadyRegistered) {
                entityBinding.addFilter(
                        ResourceVisibilityFilterManager.FILTER_NAME,
                        condition,
                        true,
                        Map.of(),
                        Map.of());
                registered++;
            }
        }

        return registered;
    }
}
