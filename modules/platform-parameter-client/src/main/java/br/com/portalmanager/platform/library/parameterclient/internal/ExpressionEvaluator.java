package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.parameterclient.internal.ExpressionContract.Operator;
import br.com.portalmanager.platform.library.parameterclient.internal.ExpressionContract.ValueType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Implements only the producer's flat V1 expression schema, without coercion. */
final class ExpressionEvaluator {
    private static final BigDecimal DECIMAL_STEP = new BigDecimal("0.01");

    static boolean evaluate(JsonNode envelope, Object input) {
        Expression expression = readExpression(envelope);
        if (expression.operator().isMembership()) {
            return evaluateMembership(expression, input);
        }
        return evaluateScalar(expression, input);
    }

    private static Expression readExpression(JsonNode envelope) {
        if (!envelope.isObject() || !"EXPRESSION".equals(envelope.path("type").asString())) {
            throw ParameterErrors.expressionInvalid();
        }

        JsonNode expression = envelope.path("value");
        if (!expression.isObject() || expression.size() != 3
                || !expression.path("operator").isString()
                || !expression.path("valueType").isString()
                || !expression.has("value")) {
            throw ParameterErrors.expressionInvalid();
        }

        Operator operator = Operator.fromPublished(expression.get("operator").asString());
        ValueType valueType = ValueType.fromPublished(expression.get("valueType").asString());
        if (operator.isOrdering() && valueType == ValueType.STRING) {
            throw ParameterErrors.expressionInvalid();
        }
        return new Expression(operator, valueType, expression.get("value"));
    }

    private static boolean evaluateMembership(Expression expression, Object input) {
        Set<Object> configuredValues = readConfiguredValues(expression.value(), expression.valueType());
        Object inputValue = readInputValue(input, expression.valueType());
        boolean contains = configuredValues.contains(inputValue);
        return expression.operator() == Operator.CONTAINS ? contains : !contains;
    }

    private static Set<Object> readConfiguredValues(JsonNode array, ValueType valueType) {
        if (!array.isArray() || array.isEmpty()) throw ParameterErrors.expressionInvalid();

        Set<Object> values = new HashSet<>();
        for (JsonNode item : array) {
            Object value = readConfiguredValue(item, valueType);
            if (!values.add(value)) throw ParameterErrors.expressionInvalid();
        }
        return values;
    }

    private static boolean evaluateScalar(Expression expression, Object input) {
        Object configuredValue = readConfiguredValue(expression.value(), expression.valueType());
        Object inputValue = readInputValue(input, expression.valueType());
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

    private static Object readConfiguredValue(JsonNode node, ValueType valueType) {
        if (valueType == ValueType.STRING) {
            if (!node.isString()) throw ParameterErrors.expressionInvalid();
            return node.asString();
        }
        if (!node.isNumber()) throw ParameterErrors.expressionInvalid();

        BigDecimal number = node.decimalValue();
        if (!isValidNumber(number, valueType)) throw ParameterErrors.expressionInvalid();
        return number.stripTrailingZeros();
    }

    private static Object readInputValue(Object input, ValueType valueType) {
        if (valueType == ValueType.STRING) {
            if (!(input instanceof String)) throw ParameterErrors.inputIncompatible();
            return input;
        }

        BigDecimal number = inputNumber(input, valueType);
        // Only the published DECIMAL is restricted to hundredths; input may have finer precision.
        if (valueType != ValueType.DECIMAL && !isValidNumber(number, valueType)) {
            throw ParameterErrors.inputIncompatible();
        }
        return number.stripTrailingZeros();
    }

    private static BigDecimal inputNumber(Object input, ValueType valueType) {
        // Integer widths can widen exactly; floating-point inputs require an explicit BigDecimal.
        return switch (input) {
            case Byte number -> BigDecimal.valueOf(number.longValue());
            case Short number -> BigDecimal.valueOf(number.longValue());
            case Integer number -> BigDecimal.valueOf(number.longValue());
            case Long number -> BigDecimal.valueOf(number);
            case BigInteger number -> new BigDecimal(number);
            case BigDecimal number when valueType == ValueType.DECIMAL -> number;
            default -> throw ParameterErrors.inputIncompatible();
        };
    }

    private static boolean isValidNumber(BigDecimal number, ValueType valueType) {
        if (valueType == ValueType.DECIMAL) {
            return number.remainder(DECIMAL_STEP).signum() == 0;
        }
        if (number.stripTrailingZeros().scale() > 0) return false;
        return valueType == ValueType.INTEGER
                ? withinRange(number, Integer.MIN_VALUE, Integer.MAX_VALUE)
                : withinRange(number, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean withinRange(BigDecimal number, long minimum, long maximum) {
        return number.compareTo(BigDecimal.valueOf(minimum)) >= 0
                && number.compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }

    private record Expression(Operator operator, ValueType valueType, JsonNode value) {}
}
