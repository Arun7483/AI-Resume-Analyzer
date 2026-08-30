package com.resumeanalyzer.service;

import com.resumeanalyzer.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {
    private final ObjectProvider<JavaMailSender> mailSender;
    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    public void send(User user) {
        String link = backendUrl.replaceAll("/$", "") + "/api/v1/auth/verify?token=" + user.getVerificationToken();
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP is not configured. Verify {} using this local link: {}", user.getEmail(), link);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your ResumePulse account");
        message.setText("Hello " + user.getFullName() + ",\n\nVerify your ResumePulse account here:\n" + link + "\n\nThis link expires in 24 hours.");
        sender.send(message);
    }
}
