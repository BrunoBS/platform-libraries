package br.com.portalmanager.platform.library.parameterclient.internal;

/** Published V1 identifiers, kept internal to the parameter client. */
final class ExpressionContract {
    private ExpressionContract() {}

    enum Operator {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN,
        LESS_THAN_OR_EQUALS;

        static Operator fromPublished(String value) {
            try {
                return valueOf(value);
            } catch (IllegalArgumentException failure) {
                throw ParameterErrors.operatorInvalid();
            }
        }

        boolean isMembership() {
            return this == CONTAINS || this == NOT_CONTAINS;
        }

        boolean isOrdering() {
            return switch (this) {
                case GREATER_THAN, GREATER_THAN_OR_EQUALS, LESS_THAN, LESS_THAN_OR_EQUALS -> true;
                default -> false;
            };
        }
    }

    enum ValueType {
        STRING,
        INTEGER,
        LONG,
        DECIMAL;

        static ValueType fromPublished(String value) {
            try {
                return valueOf(value);
            } catch (IllegalArgumentException failure) {
                throw ParameterErrors.expressionInvalid();
            }
        }
    }
}
