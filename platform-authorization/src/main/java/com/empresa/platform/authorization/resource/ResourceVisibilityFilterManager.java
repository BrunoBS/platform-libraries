package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.model.ParsedGroup;
import com.empresa.platform.authorization.model.UserSession;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.UnknownFilterException;

import java.util.List;

public class ResourceVisibilityFilterManager {

    public static final String FILTER_NAME = "platformResourceVisibility";
    public static final String PARAMETER_NAME = "authorizerGroups";
    public static final String NO_AUTHORIZER = "__PLATFORM_NO_AUTHORIZER__";

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
        try {
            Filter filter = session.enableFilter(FILTER_NAME);
            filter.setParameterList(PARAMETER_NAME, authorizers);
            return true;
        } catch (UnknownFilterException exception) {
            return false;
        }
    }

    public void disable() {
        entityManager.unwrap(Session.class).disableFilter(FILTER_NAME);
    }
}
