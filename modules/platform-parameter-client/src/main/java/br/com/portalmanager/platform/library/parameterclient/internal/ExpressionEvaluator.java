package br.com.portalmanager.platform.library.parameterclient.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Implements only the producer's flat V1 expression schema, without coercion. */
final class ExpressionEvaluator {
    private static final Set<String> OPERATORS = Set.of("EQUALS", "NOT_EQUALS", "CONTAINS", "NOT_CONTAINS",
            "GREATER_THAN", "GREATER_THAN_OR_EQUALS", "LESS_THAN", "LESS_THAN_OR_EQUALS");
    private static final Set<String> TYPES = Set.of("STRING", "INTEGER", "LONG", "DECIMAL");

    static boolean evaluate(JsonNode envelope, Object input) {
        if (!envelope.isObject() || !"EXPRESSION".equals(envelope.path("type").asString()))
            throw ParameterErrors.expressionInvalid();
        JsonNode expression = envelope.path("value");
        if (!expression.isObject() || expression.size() != 3
                || !expression.path("operator").isString() || !expression.path("valueType").isString()
                || !expression.has("value")) throw ParameterErrors.expressionInvalid();
        String operator = expression.get("operator").asString();
        String type = expression.get("valueType").asString();
        if (!OPERATORS.contains(operator)) throw ParameterErrors.operatorInvalid();
        if (!TYPES.contains(type)) throw ParameterErrors.expressionInvalid();
        boolean membership = operator.equals("CONTAINS") || operator.equals("NOT_CONTAINS");
        boolean ordering = operator.startsWith("GREATER_") || operator.startsWith("LESS_");
        if (ordering && type.equals("STRING")) throw ParameterErrors.expressionInvalid();
        JsonNode value = expression.get("value");
        Object configured;
        if (membership) {
            if (!value.isArray() || value.isEmpty()) throw ParameterErrors.expressionInvalid();
            var items = new HashSet<Object>();
            for (JsonNode item : value) {
                if (!items.add(operand(item, type))) throw ParameterErrors.expressionInvalid();
            }
            configured = items;
        } else {
            configured = operand(value, type);
        }
        Object actual = input(input, type);
        if (membership) {
            boolean contains = ((Set<?>) configured).contains(actual);
            return operator.equals("CONTAINS") ? contains : !contains;
        }
        int comparison = actual instanceof String text ? text.compareTo((String) configured)
                : ((BigDecimal) actual).compareTo((BigDecimal) configured);
        return switch (operator) {
            case "EQUALS" -> comparison == 0;
            case "NOT_EQUALS" -> comparison != 0;
            case "GREATER_THAN" -> comparison > 0;
            case "GREATER_THAN_OR_EQUALS" -> comparison >= 0;
            case "LESS_THAN" -> comparison < 0;
            case "LESS_THAN_OR_EQUALS" -> comparison <= 0;
            default -> throw ParameterErrors.operatorInvalid();
        };
    }

    private static Object operand(JsonNode node, String type) {
        if (type.equals("STRING")) {
            if (!node.isString()) throw ParameterErrors.expressionInvalid();
            return node.asString();
        }
        if (!node.isNumber()) throw ParameterErrors.expressionInvalid();
        BigDecimal number = node.decimalValue();
        if (!valid(number, type)) throw ParameterErrors.expressionInvalid();
        return number.stripTrailingZeros();
    }

    private static Object input(Object input, String type) {
        if (type.equals("STRING")) {
            if (!(input instanceof String)) throw ParameterErrors.inputIncompatible();
            return input;
        }
        // Integer widths can widen exactly; floating-point inputs require an explicit BigDecimal.
        BigDecimal number = switch (input) {
            case Byte n -> BigDecimal.valueOf(n.longValue());
            case Short n -> BigDecimal.valueOf(n.longValue());
            case Integer n -> BigDecimal.valueOf(n.longValue());
            case Long n -> BigDecimal.valueOf(n);
            case BigInteger n -> new BigDecimal(n);
            case BigDecimal n when type.equals("DECIMAL") -> n;
            default -> throw ParameterErrors.inputIncompatible();
        };
        if (!type.equals("DECIMAL") && !valid(number, type)) throw ParameterErrors.inputIncompatible();
        return number.stripTrailingZeros();
    }

    private static boolean valid(BigDecimal value, String type) {
        if (type.equals("DECIMAL")) return value.remainder(new BigDecimal("0.01")).signum() == 0;
        if (value.stripTrailingZeros().scale() > 0) return false;
        BigDecimal min = BigDecimal.valueOf(type.equals("INTEGER") ? Integer.MIN_VALUE : Long.MIN_VALUE);
        BigDecimal max = BigDecimal.valueOf(type.equals("INTEGER") ? Integer.MAX_VALUE : Long.MAX_VALUE);
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
