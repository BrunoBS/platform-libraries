package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.model.ParsedGroup;
import com.empresa.platform.authorization.model.UserSession;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.UnknownFilterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.util.List;

public class ResourceVisibilityFilterManager {

    private static final Logger log = LoggerFactory.getLogger(ResourceVisibilityFilterManager.class);

    public static final String FILTER_NAME = "platformResourceVisibility";
    public static final String PARAMETER_NAME = "authorizerGroups";
    public static final String NO_AUTHORIZER = "__PLATFORM_NO_AUTHORIZER__";
    public static final String NATIVE_SESSION_VARIABLE = "@platform_resource_visibility_authorizers";

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
        bindNativeAuthorizers(session, authorizers);
        log.info("[RESOURCE-VISIBILITY-POC] enable session={} open={} joinedTx={} authorizers={}",
                System.identityHashCode(session), session.isOpen(), entityManager.isJoinedToTransaction(), authorizers);
        try {
            Filter filter = session.enableFilter(FILTER_NAME);
            filter.setParameterList(PARAMETER_NAME, authorizers);
            log.info("[RESOURCE-VISIBILITY-POC] enabled session={} filterPresent={}",
                    System.identityHashCode(session), session.getEnabledFilter(FILTER_NAME) != null);
            return true;
        } catch (UnknownFilterException exception) {
            log.warn("[RESOURCE-VISIBILITY-POC] filter definition not found session={}", System.identityHashCode(session));
            return false;
        }
    }

    public void disable() {
        Session session = entityManager.unwrap(Session.class);
        log.info("[RESOURCE-VISIBILITY-POC] disable session={} filterPresentBefore={}",
                System.identityHashCode(session), session.getEnabledFilter(FILTER_NAME) != null);
        session.disableFilter(FILTER_NAME);
        clearNativeAuthorizers(session);
    }

    private void bindNativeAuthorizers(Session session, List<String> authorizers) {
        session.doWork(connection -> {
            String placeholders = String.join(", ", java.util.Collections.nCopies(authorizers.size(), "?"));
            String sql = "SET " + NATIVE_SESSION_VARIABLE + " = JSON_ARRAY(" + placeholders + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < authorizers.size(); index++) {
                    statement.setString(index + 1, authorizers.get(index));
                }
                statement.execute();
            }
        });
        log.info("[RESOURCE-VISIBILITY-POC] native authorizers bound session={} count={}",
                System.identityHashCode(session), authorizers.size());
    }

    private void clearNativeAuthorizers(Session session) {
        session.doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SET " + NATIVE_SESSION_VARIABLE + " = NULL")) {
                statement.execute();
            }
        });
        log.info("[RESOURCE-VISIBILITY-POC] native authorizers cleared session={}",
                System.identityHashCode(session));
    }
}
