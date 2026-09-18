package com.empresa.platform.authorization.resource;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

        Set<String> protectedJoinedQualifiers = new HashSet<>();
        for (Join join : select.getJoins()) {
            FromItem rightItem = join.getRightItem();
            if (!(rightItem instanceof Table joinedTable)) {
                throw unsupported("Complex JOIN items are not supported yet");
            }

            if (metadataRegistry.findByTableName(joinedTable.getName()).isEmpty()) {
                continue;
            }

            protectedJoinedQualifiers.add(normalizedQualifier(joinedTable));
        }

        if (protectedJoinedQualifiers.isEmpty()) {
            return;
        }

        for (SelectItem<?> selectItem : select.getSelectItems()) {
            if (referencesProtectedJoinedResource(selectItem.getExpression(), protectedJoinedQualifiers)) {
                throw unsupported(
                        "Protected joined resource projection is not supported with root-only visibility"
                );
            }
        }
    }

    private boolean referencesProtectedJoinedResource(
            Expression expression,
            Set<String> protectedJoinedQualifiers) {
        boolean[] protectedReference = {false};

        expression.accept(new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Column column, S context) {
                Table table = column.getTable();
                if (table != null && table.getName() != null
                        && protectedJoinedQualifiers.contains(table.getName().toLowerCase(Locale.ROOT))) {
                    protectedReference[0] = true;
                }
                return null;
            }

            @Override
            public <S> Void visit(AllColumns allColumns, S context) {
                protectedReference[0] = true;
                return null;
            }

            @Override
            public <S> Void visit(AllTableColumns allTableColumns, S context) {
                Table table = allTableColumns.getTable();
                if (table != null && table.getName() != null
                        && protectedJoinedQualifiers.contains(table.getName().toLowerCase(Locale.ROOT))) {
                    protectedReference[0] = true;
                }
                return null;
            }
        }, null);

        return protectedReference[0];
    }

    private String normalizedQualifier(Table table) {
        String qualifier = table.getAlias() != null ? table.getAlias().getName() : table.getName();
        return qualifier.toLowerCase(Locale.ROOT);
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
