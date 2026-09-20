package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.exception.UnauthorizedAccessException;
import com.empresa.platform.authorization.message.AuthorizationMessageKeys;
import com.empresa.platform.authorization.model.ParsedGroup;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;

import java.util.List;
import java.util.Locale;

/**
 * Exposes the current resource-visibility values for explicit repository query binding.
 *
 * <p>ORM queries can remain protected by the Hibernate filter. Native queries may
 * bind {@link #authorizerGroups()} and {@link #bypass()} explicitly instead of
 * relying on SQL rewriting.</p>
 */
public class ResourceVisibilityQueryContext {

    public boolean bypass() {
        return currentSession().isOwner();
    }

    public List<String> authorizerGroups() {
        List<String> authorizers = currentSession().getAuthorizerGroups().stream()
                .map(ParsedGroup::authorizer)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        return authorizers.isEmpty()
                ? List.of(ResourceVisibilityFilterManager.NO_AUTHORIZER)
                : authorizers;
    }

    private UserSession currentSession() {
        return UserContext.get()
                .orElseThrow(() -> new UnauthorizedAccessException(
                        AuthorizationMessageKeys.SESSION_NOT_FOUND
                ));
    }
}
