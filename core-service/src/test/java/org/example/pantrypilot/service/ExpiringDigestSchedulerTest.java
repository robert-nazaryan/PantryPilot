package org.example.pantrypilot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.example.pantrypilot.event.DomainEventPublisher;
import org.example.pantrypilot.event.PantryItemExpiringEvent;
import org.example.pantrypilot.event.PantryItemExpiringEvent.ExpiringItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiringDigestSchedulerTest {

    @Mock private ExpiringDigestService digestService;
    @Mock private DomainEventPublisher publisher;
    @InjectMocks private ExpiringDigestScheduler scheduler;

    @Test
    void publishDailyDigests_zeroEvents_publishesNothing() {
        ReflectionTestUtils.setField(scheduler, "windowDays", 3);
        when(digestService.buildDigests(3)).thenReturn(List.of());

        scheduler.publishDailyDigests();

        verifyNoInteractions(publisher);
    }

    @Test
    void publishDailyDigests_multipleEvents_publishesEach() {
        ReflectionTestUtils.setField(scheduler, "windowDays", 5);
        PantryItemExpiringEvent e1 = sampleEvent(1L, "alice@example.com");
        PantryItemExpiringEvent e2 = sampleEvent(2L, "bob@example.com");
        when(digestService.buildDigests(5)).thenReturn(List.of(e1, e2));

        scheduler.publishDailyDigests();

        verify(publisher).publishOutsideTransaction(e1);
        verify(publisher).publishOutsideTransaction(e2);
        verify(publisher, times(2)).publishOutsideTransaction(org.mockito.ArgumentMatchers.any());
    }

    private static PantryItemExpiringEvent sampleEvent(Long userId, String email) {
        return new PantryItemExpiringEvent(
                UUID.randomUUID(),
                java.time.Instant.now(),
                userId,
                email,
                null,
                List.of(new ExpiringItem("Milk", BigDecimal.ONE, "L", LocalDate.now().plusDays(1))));
    }
}
