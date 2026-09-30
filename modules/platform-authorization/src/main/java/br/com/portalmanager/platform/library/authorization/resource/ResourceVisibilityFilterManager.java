package br.com.portalmanager.platform.library.authorization.resource;

import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Controls the lifecycle of the Hibernate resource visibility filter in the
 * current persistence context.
 */
public class ResourceVisibilityFilterManager {

    public static final String FILTER_NAME = "platformResourceVisibility";
    public static final String PARAMETER_NAME = "authorizerGroups";
    public static final int MAX_AUTHORIZER_GROUPS = 500;
    static final String NO_AUTHORIZER = "__PLATFORM_NO_AUTHORIZER__";

    private final EntityManager entityManager;

    public ResourceVisibilityFilterManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean enable(Collection<String> authorizerGroups) {
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
        Filter filter = session.enableFilter(FILTER_NAME);
        filter.setParameterList(PARAMETER_NAME, normalized);
        return true;
    }

    public void disable() {
        Session session = entityManager.unwrap(Session.class);
        if (session.getEnabledFilter(FILTER_NAME) != null) {
            session.disableFilter(FILTER_NAME);
        }
    }
}
