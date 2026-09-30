package br.com.portalmanager.platform.library.authorization.resource;

import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;

/**
 * Controls the lifecycle of the Hibernate resource visibility filter in the
 * current persistence context.
 */
public class ResourceVisibilityFilterManager {

    public static final String FILTER_NAME = "platformResourceVisibility";
    public static final String PARAMETER_NAME = "resourceVisibilityIds";
    static final long NO_RESOURCE_ID = Long.MIN_VALUE;

    private final EntityManager entityManager;

    public ResourceVisibilityFilterManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean enable(Collection<Long> authorizedIds) {
        List<Long> ids = authorizedIds == null || authorizedIds.isEmpty()
                ? List.of(NO_RESOURCE_ID)
                : authorizedIds.stream().distinct().toList();

        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter(FILTER_NAME);
        filter.setParameterList(PARAMETER_NAME, ids);
        return true;
    }

    public void disable() {
        Session session = entityManager.unwrap(Session.class);
        if (session.getEnabledFilter(FILTER_NAME) != null) {
            session.disableFilter(FILTER_NAME);
        }
    }
}
