package com.resumeanalyzer.service;

import com.resumeanalyzer.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    private final ObjectProvider<JavaMailSender> mailSender;
    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${MAIL_FROM:${spring.mail.username:}}")
    private String mailFrom;

        @Value("${RESEND_API_KEY:}")
        private String resendApiKey;

        private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.resend.com")
            .build();

    public void send(User user) {
        String link = backendUrl.replaceAll("/$", "") + "/api/v1/auth/verify?token=" + user.getVerificationToken();
        sendMessage(user.getEmail(), user.getFullName(), "Verify your ResumePulse account", "Verify your ResumePulse account here:\n" + link + "\n\nThis link expires in 24 hours.");
    }

    public void sendPasswordReset(User user) {
        String link = frontendUrl.replaceAll("/$", "") + "/auth?resetToken=" + user.getPasswordResetToken();
        sendMessage(user.getEmail(), user.getFullName(), "Reset your ResumePulse password", "Reset your ResumePulse password here:\n" + link + "\n\nThis link expires in 30 minutes.");
    }

    private void sendMessage(String recipient, String fullName, String subject, String text) {

        if (!resendApiKey.isBlank()) {
            sendWithResend(recipient, fullName, subject, text);
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP is not configured. Email could not be sent to {}", recipient);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (!mailFrom.isBlank()) {
            message.setFrom(mailFrom);
        }
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText("Hello " + fullName + ",\n\n" + text);
        try {
            sender.send(message);
            log.info("Email sent successfully to {}", recipient);
        } catch (MailException exception) {
            log.error("Email could not be sent to {}", recipient, exception);
            throw new IllegalStateException("Email could not be sent. Check the email configuration.", exception);
        }
    }

    private void sendWithResend(String recipient, String fullName, String subject, String text) {
        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + resendApiKey)
                    .body(java.util.Map.of(
                            "from", mailFrom,
                                "to", java.util.List.of(recipient),
                                "subject", subject,
                                "text", "Hello " + fullName + ",\n\n" + text
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email sent successfully to {} using Resend", recipient);
        } catch (RuntimeException exception) {
            log.error("Email could not be sent to {} using Resend", recipient, exception);
            throw new IllegalStateException("Email could not be sent. Check RESEND_API_KEY and MAIL_FROM.", exception);
        }
    }
}
