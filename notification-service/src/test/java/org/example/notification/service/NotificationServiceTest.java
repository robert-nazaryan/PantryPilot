package org.example.notification.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.example.notification.event.PantryItemExpiringEvent;
import org.example.notification.event.PantryItemExpiringEvent.ExpiringItem;
import org.example.notification.event.UserRegisteredEvent;
import org.example.notification.service.exception.EmailNotConfiguredException;
import org.example.notification.service.exception.EmailSendFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private SmtpEmailClient emailClient;

    private NotificationService service;
    private EventDeduper deduper;

    @BeforeEach
    void setUp() {
        deduper = new EventDeduper();
        service = new NotificationService(templateEngine(), emailClient, deduper);
    }

    @Test
    void handleUserRegistered_sendsEmailWithDisplayNameInSubjectAndBody() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), 1L, "alice@example.com", "Alice");
        doNothing().when(emailClient).send(any(), any(), any());

        service.handleUserRegistered(event);

        verify(emailClient).send(eq("alice@example.com"), contains("Welcome"), contains("Alice"));
    }

    @Test
    void handleUserRegistered_missingDisplayName_fallsBackToEmailPrefix() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), 1L, "bob@example.com", null);
        doNothing().when(emailClient).send(any(), any(), any());

        service.handleUserRegistered(event);

        verify(emailClient).send(eq("bob@example.com"), any(), contains("bob"));
    }

    @Test
    void handleUserRegistered_duplicateEventId_sendsOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId, Instant.now(), 1L, "alice@example.com", "Alice");
        doNothing().when(emailClient).send(any(), any(), any());

        service.handleUserRegistered(event);
        service.handleUserRegistered(event);

        verify(emailClient, times(1)).send(any(), any(), any());
    }

    @Test
    void handleUserRegistered_notConfigured_swallowsAndDoesNotThrow() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), 1L, "alice@example.com", "Alice");
        doThrow(new EmailNotConfiguredException()).when(emailClient).send(any(), any(), any());

        service.handleUserRegistered(event);
    }

    @Test
    void handleUserRegistered_sendFailure_propagatesForDltRouting() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), 1L, "alice@example.com", "Alice");
        doThrow(new EmailSendFailedException("boom")).when(emailClient).send(any(), any(), any());

        assertThatThrownBy(() -> service.handleUserRegistered(event))
                .isInstanceOf(EmailSendFailedException.class);
    }

    @Test
    void handleUserRegistered_firstSendFails_thenRetrySucceedsAndActuallySends() {
        UUID eventId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId, Instant.now(), 1L, "alice@example.com", "Alice");
        doThrow(new EmailSendFailedException("transient"))
                .doNothing()
                .when(emailClient).send(any(), any(), any());

        assertThatThrownBy(() -> service.handleUserRegistered(event))
                .isInstanceOf(EmailSendFailedException.class);
        service.handleUserRegistered(event);

        verify(emailClient, times(2)).send(eq("alice@example.com"), any(), any());
    }

    @Test
    void handlePantryItemExpiring_sendsEmailListingItems() {
        PantryItemExpiringEvent event = new PantryItemExpiringEvent(
                UUID.randomUUID(), Instant.now(), 1L, "alice@example.com", "Alice",
                List.of(new ExpiringItem("Milk", BigDecimal.ONE, "L", LocalDate.now().plusDays(2))));
        doNothing().when(emailClient).send(any(), any(), any());

        service.handlePantryItemExpiring(event);

        verify(emailClient).send(eq("alice@example.com"), contains("expiring"), contains("Milk"));
    }

    @Test
    void handlePantryItemExpiring_duplicateEventId_sendsOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        PantryItemExpiringEvent event = new PantryItemExpiringEvent(
                eventId, Instant.now(), 1L, "alice@example.com", "Alice",
                List.of(new ExpiringItem("Milk", BigDecimal.ONE, "L", LocalDate.now().plusDays(2))));
        doNothing().when(emailClient).send(any(), any(), any());

        service.handlePantryItemExpiring(event);
        service.handlePantryItemExpiring(event);

        verify(emailClient, times(1)).send(any(), any(), any());
    }

    @Test
    void handleUserRegistered_differentEventIds_sendsEachOnce() {
        UserRegisteredEvent a = new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), 1L, "alice@example.com", "Alice");
        UserRegisteredEvent b = new UserRegisteredEvent(
                UUID.randomUUID(), Instant.now(), 2L, "bob@example.com", "Bob");
        doNothing().when(emailClient).send(any(), any(), any());

        service.handleUserRegistered(a);
        service.handleUserRegistered(b);

        verify(emailClient, times(2)).send(any(), any(), any());
        verify(emailClient, never()).send(eq("nobody@example.com"), any(), any());
    }

    private static TemplateEngine templateEngine() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setApplicationContext(new org.springframework.context.support.StaticApplicationContext());
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

}
