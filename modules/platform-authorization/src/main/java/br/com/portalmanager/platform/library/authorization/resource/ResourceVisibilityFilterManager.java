package br.com.portalmanager.platform.library.authorization.resource;

import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;

import java.util.List;

/**
 * POC: controls the lifecycle of the Hibernate resource visibility filter in the
 * current persistence context. The filter definition/mapping is intentionally
 * kept outside domain entities and will be registered by the library bootstrap.
 */
public class ResourceVisibilityFilterManager {

    public static final String FILTER_NAME = "platformResourceVisibility";
    public static final String PARAMETER_NAME = "authorizerGroups";
    static final String NO_AUTHORIZER = "__PLATFORM_NO_AUTHORIZER__";

    private final EntityManager entityManager;

    public ResourceVisibilityFilterManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean enable(UserSession userSession) {
        if (userSession.isOwner()) {
            return false;
        }

        List<String> authorizers = userSession.getAuthorizerGroups().stream()
                .map(ParsedGroup::authorizer)
                .filter(value -> value != null && !value.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        if (authorizers.isEmpty()) {
            authorizers = List.of(NO_AUTHORIZER);
        }

        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter(FILTER_NAME);
        filter.setParameterList(PARAMETER_NAME, authorizers);
        return true;
    }

    public void disable() {
        Session session = entityManager.unwrap(Session.class);
        if (session.getEnabledFilter(FILTER_NAME) != null) {
            session.disableFilter(FILTER_NAME);
        }
    }
}
