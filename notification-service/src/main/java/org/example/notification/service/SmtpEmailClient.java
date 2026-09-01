package org.example.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.example.notification.config.MailProperties;
import org.example.notification.service.exception.EmailNotConfiguredException;
import org.example.notification.service.exception.EmailSendFailedException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpEmailClient {

    private final JavaMailSender mailSender;
    private final MailProperties props;

    public void send(String toEmail, String subject, String htmlBody) {
        if (!props.isConfigured()) {
            throw new EmailNotConfiguredException();
        }
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(props.fromEmail());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
        } catch (MessagingException ex) {
            throw new EmailSendFailedException("Failed to build MimeMessage", ex);
        }
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new EmailSendFailedException("SMTP send failed", ex);
        }
    }
}
