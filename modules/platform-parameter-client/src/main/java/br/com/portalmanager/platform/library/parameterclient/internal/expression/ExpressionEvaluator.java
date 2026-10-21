package br.com.portalmanager.platform.library.parameterclient.internal.expression;

import br.com.portalmanager.platform.library.parameterclient.internal.ParameterErrors;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ExpressionOperator;
import java.math.BigDecimal;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Coordinates V1 validation and compares the input against published operands. */
public final class ExpressionEvaluator {
    private ExpressionEvaluator() {}

    public static boolean evaluate(JsonNode envelope, Object input) {
        ExpressionDocument expression = ExpressionDocumentReader.read(envelope);
        if (expression.operator().isMembership()) {
            return evaluateMembership(expression, input);
        }
        return evaluateScalar(expression, input);
    }

    private static boolean evaluateMembership(ExpressionDocument expression, Object input) {
        Set<Object> configuredValues = ExpressionValues.readConfiguredValues(expression.value(), expression.valueType());
        Object inputValue = ExpressionValues.readInputValue(input, expression.valueType());
        boolean contains = configuredValues.contains(inputValue);
        return expression.operator() == ExpressionOperator.CONTAINS ? contains : !contains;
    }

    private static boolean evaluateScalar(ExpressionDocument expression, Object input) {
        Object configuredValue = ExpressionValues.readConfiguredValue(expression.value(), expression.valueType());
        Object inputValue = ExpressionValues.readInputValue(input, expression.valueType());
        int comparison = compare(inputValue, configuredValue);
        return switch (expression.operator()) {
            case EQUALS -> comparison == 0;
            case NOT_EQUALS -> comparison != 0;
            case GREATER_THAN -> comparison > 0;
            case GREATER_THAN_OR_EQUALS -> comparison >= 0;
            case LESS_THAN -> comparison < 0;
            case LESS_THAN_OR_EQUALS -> comparison <= 0;
            case CONTAINS, NOT_CONTAINS -> throw ParameterErrors.operatorInvalid();
        };
    }

    private static int compare(Object inputValue, Object configuredValue) {
        if (inputValue instanceof String text) {
            return text.compareTo((String) configuredValue);
        }
        return ((BigDecimal) inputValue).compareTo((BigDecimal) configuredValue);
    }
}
