package com.escrowflow.web.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("EMAIL_NOT_VERIFIED");
    }

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
