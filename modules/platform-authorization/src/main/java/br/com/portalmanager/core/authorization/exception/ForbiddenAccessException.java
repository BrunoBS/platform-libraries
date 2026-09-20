package br.com.portalmanager.core.authorization.exception;

public class ForbiddenAccessException extends AuthorizationException {

    public ForbiddenAccessException(String code) {
        super(code);
    }

    public ForbiddenAccessException(String code, Throwable cause) {
        super(code, cause);
    }
}
