package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.AuthRequest;
import com.resumeanalyzer.dto.AuthResponse;
import com.resumeanalyzer.dto.EmailVerificationRequest;
import com.resumeanalyzer.dto.GoogleAuthRequest;
import com.resumeanalyzer.dto.PasswordResetConfirmRequest;
import com.resumeanalyzer.dto.PasswordResetRequest;
import com.resumeanalyzer.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AuthRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(
            @RequestParam String token
    ) {

        authService.verify(token);

        return ResponseEntity.ok(
                "Email verified successfully. You can now sign in."
        );
    }

        @PostMapping("/resend-verification")
        public ResponseEntity<String> resendVerification(
                        @Valid @RequestBody EmailVerificationRequest request
        ) {

                authService.resendVerification(request.email());

                return ResponseEntity.ok(
                                "If the account exists and is not verified, a new verification email has been sent."
                );
        }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

        @PostMapping("/google")
        public ResponseEntity<AuthResponse> google(
                        @Valid @RequestBody GoogleAuthRequest request
        ) {
                return ResponseEntity.ok(authService.googleLogin(request));
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<String> forgotPassword(
                        @Valid @RequestBody PasswordResetRequest request
        ) {
                authService.requestPasswordReset(request.email());
                return ResponseEntity.ok("If the account exists, a password reset email has been sent.");
        }

        @PostMapping("/reset-password")
        public ResponseEntity<String> resetPassword(
                        @Valid @RequestBody PasswordResetConfirmRequest request
        ) {
                authService.resetPassword(request.token(), request.password());
                return ResponseEntity.ok("Password reset successfully. You can now sign in.");
        }
}