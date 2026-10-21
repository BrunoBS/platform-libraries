package br.com.portalmanager.platform.library.parameterclient.internal.expression;

import br.com.portalmanager.platform.library.parameterclient.internal.ParameterErrors;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ExpressionOperator;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ExpressionValueType;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterFields;
import br.com.portalmanager.platform.library.parameterclient.internal.contract.ParameterType;
import tools.jackson.databind.JsonNode;

/** Validates document structure and resolves its published identifiers. */
final class ExpressionDocumentReader {
    private ExpressionDocumentReader() {}

    static ExpressionDocument read(JsonNode envelope) {
        if (!envelope.isObject()
                || !ParameterType.EXPRESSION.name().equals(envelope.path(ParameterFields.TYPE).asString())) {
            throw ParameterErrors.expressionInvalid();
        }

        JsonNode expression = envelope.path(ParameterFields.VALUE);
        if (!expression.isObject() || expression.size() != 3
                || !expression.path(ParameterFields.OPERATOR).isString()
                || !expression.path(ParameterFields.VALUE_TYPE).isString()
                || !expression.has(ParameterFields.VALUE)) {
            throw ParameterErrors.expressionInvalid();
        }

        ExpressionOperator operator = ExpressionOperator.fromPublished(
                expression.get(ParameterFields.OPERATOR).asString());
        ExpressionValueType valueType = ExpressionValueType.fromPublished(
                expression.get(ParameterFields.VALUE_TYPE).asString());
        if (operator.isOrdering() && valueType == ExpressionValueType.STRING) {
            throw ParameterErrors.expressionInvalid();
        }
        return new ExpressionDocument(operator, valueType, expression.get(ParameterFields.VALUE));
    }
}
