package br.com.portalmanager.platform.library.parameterclient.internal.expression;

import br.com.portalmanager.platform.library.parameterclient.internal.ParameterErrors;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ExpressionValueType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Validates configured operands and normalizes compatible inputs without rounding. */
final class ExpressionValues {
    private static final BigDecimal DECIMAL_STEP = new BigDecimal("0.01");

    private ExpressionValues() {}

    static Set<Object> readConfiguredValues(JsonNode array, ExpressionValueType valueType) {
        if (!array.isArray() || array.isEmpty()) throw ParameterErrors.expressionInvalid();

        Set<Object> values = new HashSet<>();
        for (JsonNode item : array) {
            Object value = readConfiguredValue(item, valueType);
            if (!values.add(value)) throw ParameterErrors.expressionInvalid();
        }
        return values;
    }

    static Object readConfiguredValue(JsonNode node, ExpressionValueType valueType) {
        if (valueType == ExpressionValueType.STRING) {
            if (!node.isString()) throw ParameterErrors.expressionInvalid();
            return node.asString();
        }
        if (!node.isNumber()) throw ParameterErrors.expressionInvalid();

        BigDecimal number = node.decimalValue();
        if (!isValidNumber(number, valueType)) throw ParameterErrors.expressionInvalid();
        return number.stripTrailingZeros();
    }

    static Object readInputValue(Object input, ExpressionValueType valueType) {
        if (valueType == ExpressionValueType.STRING) {
            if (!(input instanceof String)) throw ParameterErrors.inputIncompatible();
            return input;
        }

        BigDecimal number = inputNumber(input, valueType);
        // Only the published DECIMAL is restricted to hundredths; input may have finer precision.
        if (valueType != ExpressionValueType.DECIMAL && !isValidNumber(number, valueType)) {
            throw ParameterErrors.inputIncompatible();
        }
        return number.stripTrailingZeros();
    }

    private static BigDecimal inputNumber(Object input, ExpressionValueType valueType) {
        // Integer widths can widen exactly; floating-point inputs require an explicit BigDecimal.
        return switch (input) {
            case Byte number -> BigDecimal.valueOf(number.longValue());
            case Short number -> BigDecimal.valueOf(number.longValue());
            case Integer number -> BigDecimal.valueOf(number.longValue());
            case Long number -> BigDecimal.valueOf(number);
            case BigInteger number -> new BigDecimal(number);
            case BigDecimal number when valueType == ExpressionValueType.DECIMAL -> number;
            default -> throw ParameterErrors.inputIncompatible();
        };
    }

    private static boolean isValidNumber(BigDecimal number, ExpressionValueType valueType) {
        if (valueType == ExpressionValueType.DECIMAL) {
            return number.remainder(DECIMAL_STEP).signum() == 0;
        }
        if (number.stripTrailingZeros().scale() > 0) return false;
        return valueType == ExpressionValueType.INTEGER
                ? withinRange(number, Integer.MIN_VALUE, Integer.MAX_VALUE)
                : withinRange(number, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean withinRange(BigDecimal number, long minimum, long maximum) {
        return number.compareTo(BigDecimal.valueOf(minimum)) >= 0
                && number.compareTo(BigDecimal.valueOf(maximum)) <= 0;
    }
}
