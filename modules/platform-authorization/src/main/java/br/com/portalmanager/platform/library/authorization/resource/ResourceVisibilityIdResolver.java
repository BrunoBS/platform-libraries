package br.com.portalmanager.platform.library.authorization.resource;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves visible resource ids from annotation-provided source metadata.
 *
 * <p>Only SQL identifiers are interpolated into the statement. Authorizer
 * values are always bound as positional parameters.</p>
 */
public class ResourceVisibilityIdResolver {

    private static final Pattern SIMPLE_IDENTIFIER =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private static final Pattern QUALIFIED_IDENTIFIER =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    private final EntityManager entityManager;

    public ResourceVisibilityIdResolver(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Set<Long> findAuthorizedIds(
            ResourceVisibility visibility,
            Collection<String> authorizerGroups
    ) {
        if (authorizerGroups == null || authorizerGroups.isEmpty()) {
            return Set.of();
        }

        String table = requireQualifiedIdentifier(visibility.table(), "table");
        String resourceIdColumn =
                requireSimpleIdentifier(visibility.resourceIdColumn(), "resourceIdColumn");
        String authorizerGroupColumn =
                requireSimpleIdentifier(visibility.authorizerGroupColumn(), "authorizerGroupColumn");

        List<String> normalizedGroups = authorizerGroups.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        if (normalizedGroups.isEmpty()) {
            return Set.of();
        }

        List<String> placeholders = new ArrayList<>(normalizedGroups.size());
        for (int index = 1; index <= normalizedGroups.size(); index++) {
            placeholders.add("?" + index);
        }

        String sql = "SELECT " + resourceIdColumn
                + " FROM " + table
                + " WHERE lower(" + authorizerGroupColumn + ") IN ("
                + String.join(", ", placeholders)
                + ")";

        Query query = entityManager.createNativeQuery(sql);

        for (int index = 0; index < normalizedGroups.size(); index++) {
            query.setParameter(index + 1, normalizedGroups.get(index));
        }

        @SuppressWarnings("unchecked")
        List<Object> rows = query.getResultList();

        Set<Long> result = new LinkedHashSet<>();
        for (Object row : rows) {
            if (row instanceof Number number) {
                result.add(number.longValue());
            } else if (row != null) {
                result.add(Long.valueOf(row.toString()));
            }
        }
        return Set.copyOf(result);
    }

    private static String requireSimpleIdentifier(String value, String attribute) {
        if (value == null || !SIMPLE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier for " + attribute + ": " + value);
        }
        return value;
    }

    private static String requireQualifiedIdentifier(String value, String attribute) {
        if (value == null || !QUALIFIED_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier for " + attribute + ": " + value);
        }
        return value;
    }
}
