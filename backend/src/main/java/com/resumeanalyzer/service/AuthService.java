package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.AuthRequest;
import com.resumeanalyzer.dto.AuthResponse;
import com.resumeanalyzer.dto.GoogleAuthRequest;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.BadRequestException;
import com.resumeanalyzer.repository.UserRepository;
import com.resumeanalyzer.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailVerificationService verification;

        private final RestClient googleClient = RestClient.builder()
            .baseUrl("https://oauth2.googleapis.com")
            .build();

        @Value("${GOOGLE_CLIENT_ID:}")
        private String googleClientId;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String email = request.email().trim().toLowerCase();

        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new BadRequestException("Full name is required for registration");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account already exists for this email");
        }

        User user = users.save(User.builder()
                .email(email)
                .password(encoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(User.Role.ROLE_USER)
                .emailVerified(true)
                .build());

            return response(user);
    }

    @Transactional
    public void resendVerification(String rawEmail) {
        users.findByEmailIgnoreCase(rawEmail.trim().toLowerCase())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    user.setVerificationToken(UUID.randomUUID().toString());
                    user.setVerificationExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
                    verification.send(users.save(user));
                });
    }

    @Transactional
    public AuthResponse googleLogin(GoogleAuthRequest request) {
        java.util.Map<?, ?> claims;
        try {
            claims = googleClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo")
                            .queryParam("id_token", request.credential()).build())
                    .retrieve()
                    .body(java.util.Map.class);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Google sign-in could not be verified", exception);
        }

        String email = value(claims, "email");
        String name = value(claims, "name");
        String verified = value(claims, "email_verified");
        String audience = value(claims, "aud");
        if (email.isBlank() || !"true".equalsIgnoreCase(verified)
            || (!googleClientId.isBlank() && !googleClientId.equals(audience))) {
            throw new BadRequestException("Google did not verify this email address");
        }

        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> users.save(User.builder()
                .email(email.toLowerCase())
                .password(encoder.encode(UUID.randomUUID().toString()))
                .fullName(name.isBlank() ? email : name)
                .role(User.Role.ROLE_USER)
                .emailVerified(true)
                .build()));
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            users.save(user);
        }
        return response(user);
    }

    @Transactional
    public void requestPasswordReset(String rawEmail) {
        users.findByEmailIgnoreCase(rawEmail.trim().toLowerCase()).ifPresent(user -> {
            user.setPasswordResetToken(UUID.randomUUID().toString());
            user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            verification.sendPasswordReset(users.save(user));
        });
    }

    @Transactional
    public void resetPassword(String token, String password) {
        User user = users.findByPasswordResetToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset link"));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired password reset link");
        }
        user.setPassword(encoder.encode(password));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        users.save(user);
    }

    @Transactional
    public void verify(String token) {
        User user = users.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification link"));
        if (user.isEmailVerified()) {
            return;
        }
        if (user.getVerificationExpiresAt() == null
                || user.getVerificationExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This verification link has expired");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiresAt(null);
        users.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!encoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            throw new BadCredentialsException("Please verify your email before signing in");
        }
        return response(user);
    }

    private AuthResponse response(User user) {
        String token = jwt.generate(user);
        return new AuthResponse(token, "Bearer", jwt.expirationSeconds(), user.getFullName(), user.getEmail(),
            user.getRole().name(), true, "Login successful");
    }

    private String value(java.util.Map<?, ?> claims, String key) {
        Object value = claims == null ? null : claims.get(key);
        return value == null ? "" : value.toString();
    }
}
