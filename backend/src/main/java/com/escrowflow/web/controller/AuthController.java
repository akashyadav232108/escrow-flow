package com.escrowflow.web.controller;

import com.escrowflow.security.SecurityUtils;
import com.escrowflow.service.AuthService;
import com.escrowflow.web.dto.AuthResponse;
import com.escrowflow.web.dto.ChangePasswordRequest;
import com.escrowflow.web.dto.ForgotPasswordRequest;
import com.escrowflow.web.dto.LoginRequest;
import com.escrowflow.web.dto.ResendOtpRequest;
import com.escrowflow.web.dto.ResetPasswordRequest;
import com.escrowflow.web.dto.SignupRequest;
import com.escrowflow.web.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return authService.verifyEmail(request.email(), request.otp());
    }

    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request.email());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.otp(), request.newPassword());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), request);
    }
}
