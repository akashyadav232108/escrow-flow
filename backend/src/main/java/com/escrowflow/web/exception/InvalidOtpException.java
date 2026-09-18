package com.escrowflow.web.exception;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException() {
        super("INVALID_OTP");
    }

    public InvalidOtpException(String message) {
        super(message);
    }
}
