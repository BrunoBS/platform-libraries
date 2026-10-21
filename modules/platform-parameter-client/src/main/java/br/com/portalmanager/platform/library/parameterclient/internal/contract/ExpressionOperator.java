package br.com.portalmanager.platform.library.parameterclient.internal.contract;

import br.com.portalmanager.platform.library.parameterclient.internal.ParameterErrors;

/** Published V1 identifiers; internal implementation contract. */
public enum ExpressionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN,
    LESS_THAN_OR_EQUALS;

    public static ExpressionOperator fromPublished(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw ParameterErrors.operatorInvalid();
        }
    }

    public boolean isMembership() {
        return this == CONTAINS || this == NOT_CONTAINS;
    }

    public boolean isOrdering() {
        return switch (this) {
            case GREATER_THAN, GREATER_THAN_OR_EQUALS, LESS_THAN, LESS_THAN_OR_EQUALS -> true;
            default -> false;
        };
    }
}
