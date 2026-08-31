package org.example.notification.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.PantryItemExpiringEvent;
import org.example.notification.event.UserRegisteredEvent;
import org.example.notification.service.exception.EmailNotConfiguredException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String WELCOME_TEMPLATE = "welcome";
    private static final String EXPIRING_TEMPLATE = "pantry-items-expiring-digest";
    private static final String WELCOME_SUBJECT = "Welcome to PantryPilot";
    private static final String EXPIRING_SUBJECT = "Items in your pantry are expiring soon";

    private final TemplateEngine templateEngine;
    private final SmtpEmailClient emailClient;
    private final EventDeduper deduper;

    public void handleUserRegistered(UserRegisteredEvent event) {
        if (deduper.isProcessed(event.eventId())) {
            log.info("Skipping duplicate user.registered event {}", event.eventId());
            return;
        }
        Context ctx = new Context();
        ctx.setVariable("displayName", greetingName(event.displayName(), event.email()));
        String html = templateEngine.process(WELCOME_TEMPLATE, ctx);
        sendOrLogSkip(event.email(), WELCOME_SUBJECT, html, event.eventId());
    }

    public void handlePantryItemExpiring(PantryItemExpiringEvent event) {
        if (deduper.isProcessed(event.eventId())) {
            log.info("Skipping duplicate pantry.item.expiring event {}", event.eventId());
            return;
        }
        Context ctx = new Context();
        ctx.setVariable("displayName", greetingName(event.displayName(), event.email()));
        ctx.setVariable("items", event.items());
        String html = templateEngine.process(EXPIRING_TEMPLATE, ctx);
        sendOrLogSkip(event.email(), EXPIRING_SUBJECT, html, event.eventId());
    }

    private void sendOrLogSkip(String toEmail, String subject, String html, UUID eventId) {
        try {
            emailClient.send(toEmail, subject, html);
            deduper.markProcessed(eventId);
            log.info("Sent email to {} (subject='{}', eventId={})", toEmail, subject, eventId);
        } catch (EmailNotConfiguredException ex) {
            deduper.markProcessed(eventId);
            log.warn("Mail not configured; skipped email to {} (subject='{}', eventId={})",
                    toEmail, subject, eventId);
        }
    }

    private static String greetingName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "there";
    }
}
