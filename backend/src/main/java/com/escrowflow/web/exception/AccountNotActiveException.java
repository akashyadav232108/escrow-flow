package com.escrowflow.web.exception;

public class AccountNotActiveException extends RuntimeException {

    private final String errorCode;

    public AccountNotActiveException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
