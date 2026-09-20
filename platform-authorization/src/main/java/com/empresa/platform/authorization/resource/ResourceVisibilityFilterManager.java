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
    private final NativeResourceVisibilityContext nativeContext;
    private final ThreadLocal<Integer> visibilityDepth = ThreadLocal.withInitial(() -> 0);

    public ResourceVisibilityFilterManager(EntityManager entityManager,
                                             NativeResourceVisibilityContext nativeContext) {
        this.entityManager = entityManager;
        this.nativeContext = nativeContext;
    }

    public boolean enable(UserSession userSession) {
        if (userSession.isOwner()) {
            return false;
        }

        int depth = visibilityDepth.get();
        if (depth > 0) {
            visibilityDepth.set(depth + 1);
            nativeContext.enter();
            log.debug("Nested resource visibility depth={}", depth + 1);
            return true;
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
        log.debug("Enabling resource visibility session={} open={} joinedTx={} authorizerCount={}",
                System.identityHashCode(session), session.isOpen(), entityManager.isJoinedToTransaction(), authorizers.size());
        try {
            Filter filter = session.enableFilter(FILTER_NAME);
            filter.setParameterList(PARAMETER_NAME, authorizers);
            log.debug("Resource visibility enabled session={} filterPresent={}",
                    System.identityHashCode(session), session.getEnabledFilter(FILTER_NAME) != null);
            visibilityDepth.set(1);
            nativeContext.enter();
            return true;
        } catch (UnknownFilterException exception) {
            cleanupFailedEnable(session);
            log.warn("Resource visibility filter definition not found session={}", System.identityHashCode(session));
            return false;
        } catch (RuntimeException exception) {
            try {
                cleanupFailedEnable(session);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    public void disable() {
        int depth = visibilityDepth.get();
        if (depth > 1) {
            visibilityDepth.set(depth - 1);
            nativeContext.exit();
            log.debug("Nested resource visibility exit depth={}", depth - 1);
            return;
        }
        if (depth == 0) {
            return;
        }

        RuntimeException disableFailure = null;
        Session session = null;
        try {
            session = entityManager.unwrap(Session.class);
            log.debug("Disabling resource visibility session={} filterPresentBefore={}",
                    System.identityHashCode(session), session.getEnabledFilter(FILTER_NAME) != null);
            session.disableFilter(FILTER_NAME);
        } catch (RuntimeException exception) {
            disableFailure = exception;
        }

        if (session != null) {
            try {
                clearNativeAuthorizers(session);
            } catch (RuntimeException exception) {
                if (disableFailure == null) {
                    disableFailure = exception;
                } else {
                    disableFailure.addSuppressed(exception);
                }
            }
        }

        visibilityDepth.remove();
        nativeContext.exit();

        if (disableFailure != null) {
            throw disableFailure;
        }
    }

    private void cleanupFailedEnable(Session session) {
        RuntimeException cleanupFailure = null;
        try {
            if (session.getEnabledFilter(FILTER_NAME) != null) {
                session.disableFilter(FILTER_NAME);
            }
        } catch (UnknownFilterException ignored) {
            // Filter definition itself is unavailable; native state still must be cleared.
        } catch (RuntimeException exception) {
            cleanupFailure = exception;
        }

        try {
            clearNativeAuthorizers(session);
        } catch (RuntimeException exception) {
            if (cleanupFailure == null) {
                cleanupFailure = exception;
            } else {
                cleanupFailure.addSuppressed(exception);
            }
        } finally {
            visibilityDepth.remove();
        }

        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
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
        log.debug("Native resource visibility authorizers bound session={} count={}",
                System.identityHashCode(session), authorizers.size());
    }

    private void clearNativeAuthorizers(Session session) {
        session.doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SET " + NATIVE_SESSION_VARIABLE + " = NULL")) {
                statement.execute();
            }
        });
        log.debug("Native resource visibility authorizers cleared session={}",
                System.identityHashCode(session));
    }
}
