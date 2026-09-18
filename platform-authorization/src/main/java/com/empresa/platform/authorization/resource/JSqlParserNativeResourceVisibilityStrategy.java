package com.empresa.platform.authorization.resource;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;


/**
 * PoC AST strategy for simple native SELECTs. It injects the visibility predicate
 * at the protected table alias instead of requiring the visibility column in the projection.
 *
 * Complex SELECT shapes are intentionally fail-closed until explicitly supported.
 */
public class JSqlParserNativeResourceVisibilityStrategy implements NativeResourceVisibilityStrategy {

    private final ResourceVisibilityMetadataRegistry metadataRegistry;

    public JSqlParserNativeResourceVisibilityStrategy(ResourceVisibilityMetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    public String apply(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new ResourceVisibilityNativeQueryException("Protected native query must not be empty");
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select) || !(select instanceof PlainSelect plainSelect)) {
                throw unsupported("Only simple SELECT statements are supported by native resource visibility");
            }

            ProtectedTable protectedTable = findRootProtectedTable(plainSelect);
            rejectProtectedJoinedResourceProjection(plainSelect, protectedTable);
            Expression visibility = visibilityPredicate(protectedTable);

            plainSelect.setWhere(plainSelect.getWhere() == null
                    ? visibility
                    : new AndExpression(plainSelect.getWhere(), visibility));

            return statement.toString();
        } catch (ResourceVisibilityNativeQueryException exception) {
            throw exception;
        } catch (JSQLParserException | RuntimeException exception) {
            throw new ResourceVisibilityNativeQueryException(
                    "Protected native query could not be safely parsed for resource visibility"
            );
        }
    }

    private ProtectedTable findRootProtectedTable(PlainSelect select) {
        FromItem fromItem = select.getFromItem();
        if (!(fromItem instanceof Table table)) {
            throw unsupported("Subqueries and complex FROM items are not supported yet");
        }

        ResourceVisibilityMetadata metadata = metadataRegistry.findByTableName(table.getName())
                .orElseThrow(() -> unsupported(
                        "Protected native query root resource is not registered for visibility"
                ));

        return new ProtectedTable(table, metadata);
    }

    private void rejectProtectedJoinedResourceProjection(PlainSelect select, ProtectedTable root) {
        if (select.getJoins() == null || select.getJoins().isEmpty()) {
            return;
        }

        for (Join join : select.getJoins()) {
            FromItem rightItem = join.getRightItem();
            if (!(rightItem instanceof Table joinedTable)) {
                throw unsupported("Complex JOIN items are not supported yet");
            }

            ResourceVisibilityMetadata joinedMetadata = metadataRegistry.findByTableName(joinedTable.getName())
                    .orElse(null);
            if (joinedMetadata == null) {
                continue;
            }

            String joinedQualifier = joinedTable.getAlias() != null
                    ? joinedTable.getAlias().getName()
                    : joinedTable.getName();

            boolean projectsJoinedResource = select.getSelectItems().stream()
                    .map(SelectItem::toString)
                    .map(String::trim)
                    .anyMatch(item -> item.equals("*")
                            || item.regionMatches(true, 0, joinedQualifier + ".", 0, joinedQualifier.length() + 1));

            if (projectsJoinedResource) {
                throw unsupported(
                        "Protected joined resource projection is not supported with root-only visibility"
                );
            }
        }
    }

    private Expression visibilityPredicate(ProtectedTable protectedTable) {
        String qualifier = protectedTable.table().getAlias() != null
                ? protectedTable.table().getAlias().getName()
                : protectedTable.table().getName();

        Function lower = new Function();
        lower.setName("LOWER");
        lower.setParameters(new ExpressionList<>(
                new Column(new Table(qualifier), protectedTable.metadata().visibilityColumn())
        ));

        Function jsonQuote = new Function();
        jsonQuote.setName("JSON_QUOTE");
        jsonQuote.setParameters(new ExpressionList<>(lower));

        Function jsonContains = new Function();
        jsonContains.setName("JSON_CONTAINS");
        jsonContains.setParameters(new ExpressionList<>(
                new Column(ResourceVisibilityFilterManager.NATIVE_SESSION_VARIABLE),
                jsonQuote
        ));

        EqualsTo truthy = new EqualsTo();
        truthy.setLeftExpression(jsonContains);
        truthy.setRightExpression(new net.sf.jsqlparser.expression.LongValue(1));
        return truthy;
    }

    private ResourceVisibilityNativeQueryException unsupported(String message) {
        return new ResourceVisibilityNativeQueryException(message);
    }

    private record ProtectedTable(Table table, ResourceVisibilityMetadata metadata) {
    }
}
