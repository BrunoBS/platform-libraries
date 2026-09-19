package com.empresa.platform.authorization.resource;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies database-side visibility to native Spring Data queries while a
 * {@code @ResourceVisibility} scope is active.
 *
 * <p>The repository parameter annotated with {@code @ResourceVisibilityGroups}
 * is the anchor. The PoC deliberately supports one mandatory predicate in the
 * form {@code WHERE|AND <expression> IN (:parameter)}. Unsupported or ambiguous
 * forms fail closed.</p>
 */
public class ResourceVisibilityNativeQueryRewriter implements QueryRewriter {

    private final NativeResourceVisibilityContext context;
    private final NativeResourceVisibilityStrategy strategy;

    public ResourceVisibilityNativeQueryRewriter(
            NativeResourceVisibilityContext context,
            NativeResourceVisibilityStrategy strategy) {
        this.context = context;
        this.strategy = strategy;
    }

    @Override
    public String rewrite(String query, Sort sort) {
        if (!context.isActive()) {
            return query;
        }
        return strategy.apply(query);
    }

    public String rewrite(String query, Sort sort, String visibilityParameter) {
        if (!context.isActive()) {
            return query;
        }
        if (query == null || query.isBlank()) {
            throw new ResourceVisibilityNativeQueryException("Protected native query must not be empty");
        }
        if (visibilityParameter == null || visibilityParameter.isBlank()) {
            throw new ResourceVisibilityNativeQueryException("Native visibility parameter must not be blank");
        }

        Pattern pattern = visibilityPattern(visibilityParameter);
        Matcher matcher = pattern.matcher(query);

        if (!matcher.find()) {
            throw new ResourceVisibilityNativeQueryException(
                    "Native visibility query must contain a mandatory WHERE/AND predicate using IN (:" +
                            visibilityParameter + ")"
            );
        }

        String prefix = matcher.group("prefix");
        String predicate = matcher.group("predicate");

        if (matcher.find()) {
            throw new ResourceVisibilityNativeQueryException(
                    "Native visibility parameter :" + visibilityParameter +
                            " must occur in exactly one supported mandatory predicate"
            );
        }

        if (!context.isOwner()) {
            return query;
        }

        String replacement = prefix + " (TRUE = TRUE OR " + predicate + ")";
        return pattern.matcher(query).replaceFirst(Matcher.quoteReplacement(replacement));
    }

    private Pattern visibilityPattern(String visibilityParameter) {
        String parameter = Pattern.quote(visibilityParameter);

        // Intentionally narrow convention:
        //   WHERE LOWER(alias.column) IN (:groups)
        //   AND   LOWER(alias.column) IN (:groups)
        //   WHERE alias.column IN (:groups)
        //   AND   alias.column IN (:groups)
        //
        // OR is not accepted as a prefix because resource visibility is mandatory.
        String expression =
                "(?:LOWER\\s*\\(\\s*[A-Za-z0-9_$.]+\\s*\\)|[A-Za-z0-9_$.]+)";

        return Pattern.compile(
                "(?i)(?<prefix>\\b(?:WHERE|AND)\\b)\\s+" +
                        "(?<predicate>" + expression +
                        "\\s+IN\\s*\\(\\s*:" + parameter + "\\s*\\))"
        );
    }
}
