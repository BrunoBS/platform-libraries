package com.empresa.platform.authorization.exception;

public abstract class AuthorizationException extends RuntimeException {

    private final String code;

    protected AuthorizationException(String code) {
        super(code);
        this.code = code;
    }

    protected AuthorizationException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
