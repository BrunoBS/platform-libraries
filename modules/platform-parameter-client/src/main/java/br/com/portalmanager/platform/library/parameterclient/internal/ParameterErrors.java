package br.com.portalmanager.platform.library.parameterclient.internal;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;

/** Never includes keys, parameter values or original exception messages/causes. */
public final class ParameterErrors {
    private ParameterErrors() {}

    public static ValidationException invalid() {
        return new ValidationException("parameter-client.validation");
    }

    public static ApiException valueInvalid() {
        return new ApiException("parameter-client.value.invalid");
    }

    public static ApiException missing() { return new ApiException("parameter-client.missing"); }
    public static ApiException expressionInvalid() { return new ApiException("parameter-client.expression.invalid"); }
    public static ApiException operatorInvalid() { return new ApiException("parameter-client.operator.invalid"); }
    public static ApiException inputIncompatible() { return new ApiException("parameter-client.input.incompatible"); }

    public static ApiException readFailure(RuntimeException failure) {
        if (failure instanceof ApiException api) {
            return switch (api.getMessageKey()) {
                case "redis.serialization", "redis.type" -> valueInvalid();
                case "redis.validation" -> invalid();
                case "redis.timeout" -> new ApiException("parameter-client.timeout");
                default -> new ApiException("parameter-client.read.failed");
            };
        }
        return new ApiException("parameter-client.read.failed");
    }
}
