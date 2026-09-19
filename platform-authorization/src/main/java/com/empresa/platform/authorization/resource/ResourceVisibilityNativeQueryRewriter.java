package com.empresa.platform.authorization.resource;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.util.concurrent.atomic.AtomicInteger;
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

    private static final Logger log = LoggerFactory.getLogger(ResourceVisibilityNativeQueryRewriter.class);

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

        validateMandatoryVisibilityPredicate(query, visibilityParameter);

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
            log.info("ResourceVisibility native query - input:\n{}\noutput:\n{}", query, query);
            return query;
        }

        String replacement = prefix + " (TRUE = TRUE OR " + predicate + ")";
        String rewritten = pattern.matcher(query).replaceFirst(Matcher.quoteReplacement(replacement));
        log.info("ResourceVisibility native query - input:\n{}\noutput:\n{}", query, rewritten);
        return rewritten;
    }

    private void validateMandatoryVisibilityPredicate(String query, String visibilityParameter) {
        try {
            Statement statement = CCJSqlParserUtil.parse(query);
            if (!(statement instanceof Select select) || !(select instanceof PlainSelect plainSelect)) {
                throw new ResourceVisibilityNativeQueryException(
                        "Protected native query must be a simple SELECT with a WHERE clause"
                );
            }
            if (plainSelect.getWhere() == null) {
                throw new ResourceVisibilityNativeQueryException(
                        "Protected native query must contain a WHERE clause"
                );
            }

            AtomicInteger matches = new AtomicInteger();
            boolean mandatory = containsMandatoryVisibility(
                    plainSelect.getWhere(),
                    visibilityParameter,
                    true,
                    matches
            );

            if (matches.get() != 1 || !mandatory) {
                throw new ResourceVisibilityNativeQueryException(
                        "Native visibility parameter :" + visibilityParameter +
                                " must occur exactly once in a mandatory AND-only IN predicate"
                );
            }
        } catch (ResourceVisibilityNativeQueryException exception) {
            throw exception;
        } catch (JSQLParserException | RuntimeException exception) {
            throw new ResourceVisibilityNativeQueryException(
                    "Protected native query could not be safely validated for resource visibility"
            );
        }
    }

    private boolean containsMandatoryVisibility(
            Expression expression,
            String visibilityParameter,
            boolean mandatoryPath,
            AtomicInteger matches) {

        if (expression instanceof OrExpression orExpression) {
            boolean left = containsMandatoryVisibility(
                    orExpression.getLeftExpression(), visibilityParameter, false, matches);
            boolean right = containsMandatoryVisibility(
                    orExpression.getRightExpression(), visibilityParameter, false, matches);
            return left || right;
        }

        if (expression instanceof AndExpression andExpression) {
            boolean left = containsMandatoryVisibility(
                    andExpression.getLeftExpression(), visibilityParameter, mandatoryPath, matches);
            boolean right = containsMandatoryVisibility(
                    andExpression.getRightExpression(), visibilityParameter, mandatoryPath, matches);
            return left || right;
        }

        if (expression instanceof InExpression inExpression) {
            if (isVisibilityInPredicate(inExpression, visibilityParameter)) {
                matches.incrementAndGet();
                return mandatoryPath;
            }
        }

        return false;
    }

    private boolean isVisibilityInPredicate(InExpression inExpression, String visibilityParameter) {
        if (inExpression.isNot()) {
            return false;
        }

        String right = String.valueOf(inExpression.getRightExpression()).trim();
        return right.equalsIgnoreCase("(:" + visibilityParameter + ")")
                || right.equalsIgnoreCase(":" + visibilityParameter);
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
