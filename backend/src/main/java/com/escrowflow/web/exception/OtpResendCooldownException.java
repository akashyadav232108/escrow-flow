package com.escrowflow.web.exception;

public class OtpResendCooldownException extends RuntimeException {
    public OtpResendCooldownException() {
        super("OTP_RESEND_COOLDOWN");
    }

    public OtpResendCooldownException(String message) {
        super(message);
    }
}
