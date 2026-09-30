package br.com.portalmanager.platform.library.authorization.resource;

import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Controls entity-scoped Hibernate resource visibility filters in the current
 * persistence context.
 */
public class ResourceVisibilityFilterManager {

    public static final String FILTER_NAME_PREFIX = "platformResourceVisibility";
    public static final String PARAMETER_NAME = "authorizerGroups";
    public static final int MAX_AUTHORIZER_GROUPS = 500;
    static final String NO_AUTHORIZER = "__PLATFORM_NO_AUTHORIZER__";

    private final EntityManager entityManager;

    public ResourceVisibilityFilterManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static String filterName(Class<?> resourceType) {
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        return FILTER_NAME_PREFIX + "_"
                + resourceType.getName()
                        .replace('.', '_')
                        .replace('$', '_');
    }

    public boolean enable(Class<?> resourceType, Collection<String> authorizerGroups) {
        List<String> normalized = authorizerGroups == null
                ? List.of()
                : authorizerGroups.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(value -> value.toUpperCase(Locale.ROOT))
                        .distinct()
                        .toList();

        if (normalized.size() > MAX_AUTHORIZER_GROUPS) {
            throw new IllegalArgumentException(
                    "Resource visibility supports at most "
                            + MAX_AUTHORIZER_GROUPS
                            + " distinct authorizer groups"
            );
        }

        if (normalized.isEmpty()) {
            normalized = List.of(NO_AUTHORIZER);
        }

        Session session = entityManager.unwrap(Session.class);
        String filterName = filterName(resourceType);

        if (session.getEnabledFilter(filterName) != null) {
            return false;
        }

        Filter filter = session.enableFilter(filterName);
        filter.setParameterList(PARAMETER_NAME, normalized);
        return true;
    }

    public void disable(Class<?> resourceType) {
        Session session = entityManager.unwrap(Session.class);
        String filterName = filterName(resourceType);
        if (session.getEnabledFilter(filterName) != null) {
            session.disableFilter(filterName);
        }
    }
}
