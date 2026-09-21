package br.com.portalmanager.platform.authorization.exception;

public class UnauthorizedAccessException extends AuthorizationException {

    public UnauthorizedAccessException(String code) {
        super(code);
    }

    public UnauthorizedAccessException(String code, Throwable cause) {
        super(code, cause);
    }
}
