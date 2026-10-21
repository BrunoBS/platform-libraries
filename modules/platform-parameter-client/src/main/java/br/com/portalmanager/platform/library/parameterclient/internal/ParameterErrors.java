package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.parameterclient.message.ParameterClientMessageKeys;

/** Never includes keys, parameter values or original exception messages/causes. */
public final class ParameterErrors {
    private ParameterErrors() {}

    public static ValidationException invalid() {
        return new ValidationException(ParameterClientMessageKeys.VALIDATION);
    }

    public static ApiException valueInvalid() {
        return new ApiException(ParameterClientMessageKeys.VALUE_INVALID);
    }

    public static ApiException missing() {
        return new ApiException(ParameterClientMessageKeys.MISSING);
    }

    public static ApiException expressionInvalid() {
        return new ApiException(ParameterClientMessageKeys.EXPRESSION_INVALID);
    }

    public static ApiException operatorInvalid() {
        return new ApiException(ParameterClientMessageKeys.OPERATOR_INVALID);
    }

    public static ApiException inputIncompatible() {
        return new ApiException(ParameterClientMessageKeys.INPUT_INCOMPATIBLE);
    }

    public static ApiException readFailure(RuntimeException failure) {
        if (failure instanceof ApiException api) {
            return switch (api.getMessageKey()) {
                case "redis.serialization", "redis.type" -> valueInvalid();
                case "redis.validation" -> invalid();
                case "redis.timeout" -> new ApiException(ParameterClientMessageKeys.TIMEOUT);
                default -> new ApiException(ParameterClientMessageKeys.READ_FAILED);
            };
        }
        return new ApiException(ParameterClientMessageKeys.READ_FAILED);
    }
}
