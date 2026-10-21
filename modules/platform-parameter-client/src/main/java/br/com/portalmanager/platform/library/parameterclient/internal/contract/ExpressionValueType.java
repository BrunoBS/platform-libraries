package br.com.portalmanager.platform.library.parameterclient.internal.contract;

import br.com.portalmanager.platform.library.parameterclient.internal.ParameterErrors;

/** Published V1 identifiers; internal implementation contract. */
public enum ExpressionValueType {
    STRING,
    INTEGER,
    LONG,
    DECIMAL;

    public static ExpressionValueType fromPublished(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw ParameterErrors.expressionInvalid();
        }
    }
}
