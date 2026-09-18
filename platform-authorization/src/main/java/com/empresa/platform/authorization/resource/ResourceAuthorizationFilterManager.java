package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.model.ParsedGroup;
import com.empresa.platform.authorization.model.UserSession;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.UnknownFilterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Bridges the platform authorization context to Hibernate.
 *
 * <p>Domain entities that want query-time resource authorization declare a
 * Hibernate filter named {@value #FILTER_NAME} with the parameter
 * {@value #PARAMETER_NAME}. The application service only needs
 * {@code @ResourceAuthorization}; no authorization predicate is required in
 * the repository or service.</p>
 */
public class ResourceAuthorizationFilterManager {

    public static final String FILTER_NAME = "platformResourceAuthorization";
    public static final String PARAMETER_NAME = "authorizerGroups";
    public static final String NO_AUTHORIZER = "__PLATFORM_NO_AUTHORIZER__";

    private static final Logger log = LoggerFactory.getLogger(ResourceAuthorizationFilterManager.class);

    private final EntityManager entityManager;

    public ResourceAuthorizationFilterManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean enable(UserSession session) {
        if (session.isOwner()) {
            return false;
        }

        Session hibernateSession = entityManager.unwrap(Session.class);

        List<String> authorizers = session.getAuthorizerGroups().stream()
                .map(ParsedGroup::authorizer)
                .filter(value -> value != null && !value.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .toList();

        if (authorizers.isEmpty()) {
            authorizers = List.of(NO_AUTHORIZER);
        }

        try {
            Filter filter = hibernateSession.enableFilter(FILTER_NAME);
            filter.setParameterList(PARAMETER_NAME, authorizers);
            return true;
        } catch (UnknownFilterException exception) {
            log.debug(
                    "Filtro Hibernate [{}] não está mapeado neste persistence unit; mantendo validação pós-consulta.",
                    FILTER_NAME
            );
            return false;
        }
    }

    public void disable() {
        Session hibernateSession = entityManager.unwrap(Session.class);
        hibernateSession.disableFilter(FILTER_NAME);
    }
}
