package com.empresa.platform.authorization.resource;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.util.ArrayList;
import java.util.List;

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

            List<ProtectedTable> protectedTables = findProtectedTables(plainSelect);
            if (protectedTables.isEmpty()) {
                throw unsupported("Protected native query does not reference a registered visibility resource");
            }

            Expression visibility = protectedTables.stream()
                    .map(this::visibilityPredicate)
                    .reduce(AndExpression::new)
                    .orElseThrow();

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

    private List<ProtectedTable> findProtectedTables(PlainSelect select) {
        List<ProtectedTable> result = new ArrayList<>();
        collectProtectedTable(select.getFromItem(), result);
        if (select.getJoins() != null) {
            for (Join join : select.getJoins()) {
                collectProtectedTable(join.getRightItem(), result);
            }
        }
        return result;
    }

    private void collectProtectedTable(FromItem fromItem, List<ProtectedTable> result) {
        if (!(fromItem instanceof Table table)) {
            throw unsupported("Subqueries and complex FROM items are not supported yet");
        }

        metadataRegistry.resources().stream()
                .filter(metadata -> metadata.tableName().equalsIgnoreCase(table.getName()))
                .findFirst()
                .ifPresent(metadata -> result.add(new ProtectedTable(table, metadata)));
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

        MinorThanEquals truthy = new MinorThanEquals();
        truthy.setLeftExpression(new net.sf.jsqlparser.expression.LongValue(1));
        truthy.setRightExpression(jsonContains);
        return truthy;
    }

    private ResourceVisibilityNativeQueryException unsupported(String message) {
        return new ResourceVisibilityNativeQueryException(message);
    }

    private record ProtectedTable(Table table, ResourceVisibilityMetadata metadata) {
    }
}
