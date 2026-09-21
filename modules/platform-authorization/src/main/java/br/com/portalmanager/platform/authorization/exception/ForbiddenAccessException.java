package br.com.portalmanager.platform.authorization.exception;

public class ForbiddenAccessException extends AuthorizationException {

    public ForbiddenAccessException(String code) {
        super(code);
    }

    public ForbiddenAccessException(String code, Throwable cause) {
        super(code, cause);
    }
}
